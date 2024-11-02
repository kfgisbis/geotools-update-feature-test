import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.data.wfs.internal.TransactionRequest;
import org.geotools.data.wfs.internal.TransactionResponse;
import org.geotools.factory.CommonFactoryFinder;
import org.locationtech.jts.geom.Geometry;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class GeotoolsUtils {
    private final static FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    public static WFSDataStore getDataStore(String version, int port) throws IOException {
        String url = "http://localhost:" + port + "/geoserver/ows?service=wfs&request=GetCapabilities&version=" + version;

        Map<String, Object> params = Map.of(
                WFSDataStoreFactory.URL.getName(), url,
                WFSDataStoreFactory.PASSWORD.getName(), "geoserver",
                WFSDataStoreFactory.USERNAME.getName(), "admin"
        );
        return new WFSDataStoreFactory().createDataStore(params);
    }

    public static boolean updateGeometry(int port, String typeName, Geometry geometry, String sid, String wfsVersion) {
        try {
            WFSDataStore dataStore = getDataStore(wfsVersion, port);

            SimpleFeatureType sft = dataStore.getSchema(typeName);

            QName qName = dataStore.getRemoteTypeName(sft.getName());
            String geomColumn = getGeomColumn(dataStore, qName);
            Filter idFilter = ff.id(ff.featureId(sid));

            return updateTransaction(dataStore, qName, List.of(new QName(geomColumn)), List.of(geometry), idFilter);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static boolean updateTransaction(WFSDataStore dataStore, QName qName, List<QName> columns, List<Object> values, Filter filter) throws IOException {
        TransactionRequest transactionRequest = dataStore.getWfsClient().createTransaction();
        TransactionRequest.Update update = transactionRequest.createUpdate(qName, columns, values, filter);
        transactionRequest.add(update);

        return transaction(dataStore, transactionRequest, TransactionResponse::getUpdatedCount) > 0;
    }



    public static <T> T transaction(WFSDataStore dataStore, TransactionRequest transactionRequest, Function<TransactionResponse, T> action) throws IOException {
        TransactionResponse response =
                dataStore
                        .getWfsClient()
                        .issueTransaction(transactionRequest);

        return action.apply(response);
    }

    public static String getGeomColumn(WFSDataStore dataStore, QName qName) throws IOException {
        String geomColumn = dataStore
                .getRemoteSimpleFeatureType(qName)
                .getTypes()
                .stream()
                .filter(at -> at.getBinding().equals(Geometry.class))
                .map(AttributeType::getName)
                .map(Name::toString)
                .findFirst()
                .orElse(null);

        if (geomColumn == null)
            throw new RuntimeException("Geometry column not found!");

        return geomColumn;
    }
}