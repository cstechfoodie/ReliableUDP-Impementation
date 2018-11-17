package ca.concordia.httpClient.lib;

public class GetRequest extends ClientHttpRequest {

	@Override
	public String toString() {
		//String newline = System.getProperty("line.separator");
		StringBuilder bld = new StringBuilder();
		bld.append(this.getMethod().toString() + " " + this.getURI() + " " + this.getVersion() + "\r\n");
		bld.append("Host: " + this.getHost() + "\r\n");
		if(this.getHeaders().size() > 0) {
			this.getHeaders().forEach((k, v) ->{
				bld.append(k + ": " + v + "\r\n");
			});
		}
		bld.append("\r\n");
		return bld.toString();
	}

}
