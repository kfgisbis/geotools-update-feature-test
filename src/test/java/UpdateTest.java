import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdateTest {
    public static Network containerNetwork = Network.newNetwork();
    public static GenericContainer<?> POSTGIS;
    public static GenericContainer<?> GEOSERVER;
    private final static String TYPE_NAME = "bis:test";
    private static final List<String> VERSIONS = List.of("1.1.0", "2.0.0");
    private static final Geometry GEOMETRY = getGeometry();
    private static DatabaseConnector database;


    @BeforeAll
    static void init() {
        POSTGIS = new GenericContainer<>("postgis/postgis:latest")
                .withExposedPorts(5432)
                .withEnv("POSTGRES_PASSWORD", "postgres")
                .withEnv("POSTGRES_USER", "postgres")
                .withCopyFileToContainer(MountableFile.forHostPath("./create_db.sql"), "/docker-entrypoint-initdb.d/")
                .withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName("db"))
                .withNetwork(containerNetwork);

        GEOSERVER = new GenericContainer<>("docker.osgeo.org/geoserver:2.26.0")
                .withExposedPorts(8080)
                .withFileSystemBind("./geoserver_data", "/opt/geoserver_data")
                .withNetwork(containerNetwork)
                .withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName("geoserver-test"))
                .waitingFor(
                        Wait.forHttp("/geoserver/")
                                .forPort(8080)
                                .forStatusCode(200)
                                .withReadTimeout(Duration.ofSeconds(5))
                );

        POSTGIS.start();
        GEOSERVER.start();

        database = new DatabaseConnector(POSTGIS.getFirstMappedPort());
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForUpdate")
    void updateGeometry(String fid, String version) throws SQLException {
        assertTrue(GeotoolsUtils.updateGeometry(GEOSERVER.getFirstMappedPort(), TYPE_NAME, GEOMETRY, fid, version));

        String geom = database.getRowById(fid.split("\\.")[1]);

        assertEquals(geom, GEOMETRY.toText());
    }

    private static Stream<Arguments> getArgumentsForUpdate() {
        return List.of("test.0192eca6-d729-7582-d41d-3c0d38b44f24", "test.0192eca7-5bf7-778d-d41d-29318724447a")
                .stream()
                .flatMap(fid -> VERSIONS
                        .stream()
                        .map(version -> Arguments.of(fid, version)));
    }

    public static Geometry getGeometry() {
        PrecisionModel pm = new PrecisionModel();
        GeometryFactory gf = new GeometryFactory(pm, 4326);
        CoordinateArraySequence cas =
                new CoordinateArraySequence(new Coordinate[]{
                        new Coordinate(30.262776196255125, 59.93954262728195),
                        new Coordinate(30.262907623301622, 59.93958105131431),
                        new Coordinate(30.262941029017526, 59.9395526213441),
                        new Coordinate(30.262956789606722, 59.93953962130284),
                        new Coordinate(30.263027460562363, 59.939488541678394),
                        new Coordinate(30.262897264629483, 59.93944742258056),
                        new Coordinate(30.262776196255125, 59.93954262728195)
                });

        LinearRing lr = new LinearRing(cas, gf);
        return new Polygon(lr, null, gf);
    }
}