package profiler.listener.api;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ModelListener;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Component;

import java.io.IOException;

@Component(
        immediate = true,
        property = {
                // TODO enter required service properties
        },
        service = ModelListener.class
)

public class ProfilerListener extends BaseModelListener<AssetEntry>{

    //TODO make configurable options
    String ELASTIC_ENDPOINT = "http://elasticsearch:9200/";
    String SECRET = "7ea98fc6-ebc1-4f81-9be6-b6f597b9020f";
    String INDEX = "custom-profiler-agent";
    String TYPE = "_doc";

    @Override
    public void onAfterCreate(AssetEntry entry) throws ModelListenerException {

        System.out.println("prepping modellistener");

        super.onAfterCreate(entry);

        // try some reverse queries based on just title only for now ...
        String query = "{\n" +
                "  \"query\": {\n" +
                "    \"percolate\" : {\n" +
                "      \"field\" : \"query\",\n" +
                "      \"document\" : {\n" +
                "        \"body\" : \"" + entry.getTitleCurrentValue() + "\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        System.out.println("modellistener query: " + query);

        HttpUriRequest req = RequestBuilder.get(ELASTIC_ENDPOINT + INDEX + "/_search").setEntity(new StringEntity(query, ContentType.APPLICATION_JSON)).build();

        HttpClient client = HttpClientBuilder.create().build();

        try {
            HttpResponse response = client.execute(req);
            HttpEntity entity = response.getEntity();
            System.out.println("response code profilerlistener: " + response.getStatusLine().getStatusCode());
            if (response.getStatusLine().getStatusCode() == 200) {
                System.out.println(EntityUtils.toString(entity, "UTF-8"));

                JSONObject json = JSONFactoryUtil.createJSONObject(EntityUtils.toString(entity, "UTF-8"));
                // get hits and store them in agent hits object
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}