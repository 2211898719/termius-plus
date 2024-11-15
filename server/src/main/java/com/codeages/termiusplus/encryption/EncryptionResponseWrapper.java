package com.codeages.termiusplus.encryption;


import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class EncryptionResponseWrapper extends HttpServletResponseWrapper {

	private ServletOutputStream filterOutput;

	private ByteArrayOutputStream output;

	public EncryptionResponseWrapper(HttpServletResponse response) {
		super(response);
		output = new ByteArrayOutputStream();
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (filterOutput == null) {
			filterOutput = new ServletOutputStream() {
				@Override
				public void write(int b) throws IOException {
					output.write(b);
				}

				@Override
				public boolean isReady() {
					return false;
				}

				@Override
				public void setWriteListener(WriteListener writeListener) {
				}
			};
		}

		return filterOutput;
	}

	public String getResponseData() {
		return output.toString();
	}

}
