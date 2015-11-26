package lac.contextnet.sddl.arduinonode;

public class IPPort {

	private String m_ip;
	private String m_port;
	
	public IPPort()
	{
		m_ip = "10.0.0.1";
		m_port = "5000";
	}	
	
	public IPPort(String ipPort)
	{
		String[] vet = ipPort.split(":");
		
		m_ip = vet[0];
		m_port = vet[1];
	}

	static boolean IPRegexChecker(String ipPort)
	{
		String pattern = "(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})";
		return ipPort.matches(pattern);
	}
	
	public String getIP() {
		return m_ip;
	}

	public void setIP(String m_ip) {
		this.m_ip = m_ip;
	}

	public String getPort() {
		return m_port;
	}

	public void setPort(String m_port) {
		this.m_port = m_port;
	}
}