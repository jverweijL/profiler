package profiler.listener.api;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.json.JSONArray;
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
    String AGENTHIT_ENDPOINT = "http://localhost:8080/o/c/profilerhits/";
    String INDEX = "custom-profiler-agent";

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
                JSONObject json = JSONFactoryUtil.createJSONObject(EntityUtils.toString(entity, "UTF-8"));

                // get hits and store them in agent hits object
                JSONArray hits = json.getJSONObject("hits").getJSONArray("hits");

                for (Object hit : hits) {
                    JSONObject item = (JSONObject) hit;

                    String jsonhit = "{\n" +
                            "  \"agentclassPK\": " +  item.getString("_id") + ",\n" +
                            "  \"entryclassPK\": " +  entry.getClassPK() + ",\n" +
                            "  \"title\": \""+ entry.getTitleCurrentValue() + "\"\n" +
                            "}";

                    HttpUriRequest hitreq = RequestBuilder.post(AGENTHIT_ENDPOINT)
                                            .setEntity(new StringEntity(jsonhit, ContentType.APPLICATION_JSON))
                                            .addHeader("accept","application/json")
                                            .addHeader("Content-Type","application/json").build();
                    runRequest(hitreq);
                }
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (JSONException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private void runRequest(HttpUriRequest req) {
        System.out.println("Run request");
        System.out.println(req.getURI().toString());

        HttpClient client = HttpClientBuilder.create().build();

        try {
            HttpResponse response = client.execute(req);
            HttpEntity entity = response.getEntity();
            System.out.println("response code: " + response.getStatusLine().getStatusCode());
            System.out.println(response.getAllHeaders());
            //if (response.getStatusLine().getStatusCode() == 200) {
                System.out.println(EntityUtils.toString(entity, "UTF-8"));
            //}

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}