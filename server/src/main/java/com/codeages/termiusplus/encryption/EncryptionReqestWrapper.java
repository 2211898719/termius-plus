package com.codeages.termiusplus.encryption;

import com.cxytiandi.encrypt.util.StreamUtils;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EncryptionReqestWrapper extends HttpServletRequestWrapper {

	private byte[] requestBody = new byte[0];

    private Map<String, String> paramMap = new HashMap<>();

	public EncryptionReqestWrapper(HttpServletRequest request) {
		super(request);
		try {
			requestBody = StreamUtils.copyToByteArray(request.getInputStream());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(requestBody);
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return bais.read();
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {

            }
        };
	}

	public String getRequestData() {
		return new String(requestBody);
	}

	public void setRequestData(String requestData) {
		this.requestBody = requestData.getBytes();
	}

    public void setParamMap(Map<String, String> paramMap) {
        this.paramMap = paramMap;
    }

    @Override
    public String getParameter(String name) {
        return this.paramMap.get(name);
    }

    @Override
    public String[] getParameterValues(String name) {
	    if (paramMap.containsKey(name)) {
            return new String[] { getParameter(name) };
        }
        return super.getParameterValues(name);
    }
}
