package com.mydlp.ui.service;

import java.nio.ByteBuffer;

public interface EndpointSyncService {

	public void asyncRegisterEndpointMeta(String endpointAlias, String ipAddress, String usernameHash, ByteBuffer payload);
	
}
