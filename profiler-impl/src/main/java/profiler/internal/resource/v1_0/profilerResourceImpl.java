package profiler.internal.resource.v1_0;

import com.liferay.petra.http.invoker.HttpInvoker;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.vulcan.batch.engine.resource.VulcanBatchEngineImportTaskResource;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import profiler.resource.v1_0.profilerResource;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.HttpResponse;

/**
 * @author jverweij
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/profiler.properties",
	scope = ServiceScope.PROTOTYPE, service = profilerResource.class
)
public class profilerResourceImpl extends BaseprofilerResourceImpl {

	//TODO make configurable options
	String ELASTIC_ENDPOINT = "http://elasticsearch:9200/";
	String SECRET = "7ea98fc6-ebc1-4f81-9be6-b6f597b9020f";
	String INDEX = "custom-profiler-agent";
	String TYPE = "_doc";

	private final CloseableHttpClient httpClient = HttpClients.createDefault();

// doesn't seem to work
	@Override
	public Response removeAgent() throws Exception {
		System.out.println("HELLO REMOVE AGENT");

		HttpServletRequest request =
				_portal.getOriginalServletRequest(
						contextHttpServletRequest);

		String apikey = request.getHeader("x-api-key");

		if (apikey.equalsIgnoreCase(SECRET)) {
			System.out.println("REMOVE: " + request.getMethod());

			// Read from request
			StringBuilder stringBuilder = new StringBuilder();
			BufferedReader bufferedReader = null;

			try {
				InputStream inputStream = request.getInputStream();

				if (inputStream != null) {
					bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

					char[] charBuffer = new char[128];
					int bytesRead = -1;

					while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
						stringBuilder.append(charBuffer, 0, bytesRead);
					}
				} else {
					stringBuilder.append("");
				}
			} catch (IOException ex) {
				//logger.error("Error reading the request body...");
			} finally {
				if (bufferedReader != null) {
					try {
						bufferedReader.close();
					} catch (IOException ex) {
						//logger.error("Error closing bufferedReader...");
					}
				}
			}

			JSONObject json = JSONFactoryUtil.createJSONObject(stringBuilder.toString());

			if (json != null) {

				System.out.println("ID: " + json.getString("classPK"));

				HttpUriRequest req = RequestBuilder.delete(ELASTIC_ENDPOINT + INDEX + "/" + TYPE + "/" + json.getString("classPK") + "?refresh").build();

				runRequest(req);
			}
		}

		return super.removeAgent();
	}

	@Override
	public Response addAgent() throws Exception {
		HttpServletRequest request =
				_portal.getOriginalServletRequest(
						contextHttpServletRequest);

		String apikey = request.getHeader("x-api-key");

		//check whether secret matches...
		if (apikey.equalsIgnoreCase(SECRET)) {

			System.out.println("ADD/UPDATE: " + request.getMethod());

			// Read from request
			StringBuilder stringBuilder = new StringBuilder();
			BufferedReader bufferedReader = null;

			try {
				InputStream inputStream = request.getInputStream();

				if (inputStream != null) {
					bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

					char[] charBuffer = new char[128];
					int bytesRead = -1;

					while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
						stringBuilder.append(charBuffer, 0, bytesRead);
					}
				} else {
					stringBuilder.append("");
				}
			} catch (IOException ex) {
				//logger.error("Error reading the request body...");
			} finally {
				if (bufferedReader != null) {
					try {
						bufferedReader.close();
					} catch (IOException ex) {
						//logger.error("Error closing bufferedReader...");
					}
				}
			}

			JSONObject json = JSONFactoryUtil.createJSONObject(stringBuilder.toString());
			String query = json.getJSONObject("objectEntry").getJSONObject("values").getString("agentQuery");
			String name = json.getJSONObject("objectEntry").getJSONObject("values").getString("agentName");

			if (json != null) {

				String agent = "{\n" +
						"\"query\" : " + query +
						"  ,\"agentname\" : \"" + name + "\"\n" +
						"}";

				HttpUriRequest req = RequestBuilder.put(ELASTIC_ENDPOINT + INDEX + "/" + TYPE + "/" + json.getString("classPK") + "?refresh").setEntity(new StringEntity(agent, ContentType.APPLICATION_JSON)).build();

				runRequest(req);
			}
		}

		return super.addAgent();
	}

	private void runRequest(HttpUriRequest req) {
		System.out.println("Run request");
		System.out.println(req.getURI().toString());

		HttpClient client = HttpClientBuilder.create().build();

		try {
			HttpResponse response = client.execute(req);
			HttpEntity entity = response.getEntity();
			System.out.println("response code: " + response.getStatusLine().getStatusCode());
			if (response.getStatusLine().getStatusCode() == 200) {
				System.out.println(EntityUtils.toString(entity, "UTF-8"));
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Activate
	public void setMappings() throws Exception{
		//TODO reindex all agents??
		//System.out.println("Starting percolator prepare");

		// prepare mapping
		String mapping = "{\n" +
				"  \"mappings\": {\n" +
				"    \"properties\": {\n" +
				"      \"query\" : {\n" +
				"        \"type\" : \"percolator\"\n" +
				"      },\n" +
				"      \"body\" : {\n" +
				"        \"type\": \"text\",\n" +
				"		 \"analyzer\" : \"english\""+
				"      },\n" +
		        "      \"agentname\" : {\n" +
				"        \"type\": \"text\"\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";
		HttpUriRequest request = RequestBuilder.put(ELASTIC_ENDPOINT + INDEX).setEntity(new StringEntity(mapping, ContentType.APPLICATION_JSON)).build();

		runRequest(request);
	}

	@Override
	public void setVulcanBatchEngineImportTaskResource(VulcanBatchEngineImportTaskResource vulcanBatchEngineImportTaskResource) {

	}

	@Reference
	private Portal _portal;

}
