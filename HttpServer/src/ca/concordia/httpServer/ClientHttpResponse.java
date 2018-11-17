package ca.concordia.httpServer;


import java.util.HashMap;
import java.util.Map;

public class ClientHttpResponse {
	private String header;
	
	private String body;
	
	private String statusCode = "200";
	
	private String description = "OK";
	
	private String version = "HTTP/1.0";
	
	private Map<String, String> headers = new HashMap<String, String>();
	/**
	 * @return the headers
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}
	/**
	 * @param headers the headers to set
	 */
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	/**
	 * @return the header
	 */
	public String getHeader() {
		return header;
	}
	/**
	 * @param header the header to set
	 */
	public void setHeader(String header) {
		this.header = header;
	}
	/**
	 * @return the body
	 */
	public String getBody() {
		return body;
	}
	/**
	 * @param body the body to set
	 */
	public void setBody(String body) {
		this.body = body;
	}
	
	@Override
	public String toString() {
		header = version + " " + statusCode + " " + description + "\r\n";
		StringBuilder bld = new StringBuilder();
		bld.append(header).append("\r\n").append(body);
		return bld.toString();
	}
	/**
	 * @return the statusCode
	 */
	public String getStatusCode() {
		return statusCode;
	}
	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}
	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}
	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
}
