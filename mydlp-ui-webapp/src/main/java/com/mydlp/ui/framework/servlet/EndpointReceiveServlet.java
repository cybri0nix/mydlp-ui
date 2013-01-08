package com.mydlp.ui.framework.servlet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.HttpRequestHandler;

import com.mydlp.ui.framework.util.NIOUtil;
import com.mydlp.ui.service.PayloadProcessService;
import com.mydlp.ui.service.PayloadProcessService.SyncObject;
import com.mydlp.ui.thrift.MyDLPUIThriftService;

@Service("receiveServlet")
public class EndpointReceiveServlet implements HttpRequestHandler {

	private static Logger logger = LoggerFactory
			.getLogger(EndpointReceiveServlet.class);

	private static final Charset charset = Charset.forName("ISO-8859-1");
	private static final ByteBuffer errorResponse = Charset.forName("ISO-8859-1").encode(CharBuffer.wrap("error"));
	
	protected static final int MAX_CONTENT_LENGTH = 10*1024*1024;

	@Autowired
	protected MyDLPUIThriftService thriftService;
	
	@Autowired
	protected PayloadProcessService payloadProcessService;

	@Override
	public void handleRequest(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		ByteBuffer responseBuffer = null;
		try {
			if (req.getContentLength() < MAX_CONTENT_LENGTH)
			{
				ByteBuffer chunk = NIOUtil.toByteBuffer(req.getInputStream());
				SyncObject syncObject = payloadProcessService.toSyncObject(chunk);
				ByteBuffer chunkData = syncObject.getPayload();
				String operation = req.getParameter("o");
				String ipAddress = req.getRemoteAddr();
				String thriftResponse = null;
				if (operation.equals("begin")) {
					thriftResponse = thriftService.receiveBegin(ipAddress);
				} else if (operation.equals("push")) {
					Long itemId = Long.parseLong(req.getParameter("i"));
					Integer chunkNum = Integer.parseInt(req.getParameter("c"));
					Integer chunkNumTotal = Integer.parseInt(req.getParameter("t"));
					thriftResponse = thriftService.receiveChunk(ipAddress, itemId, chunkData, chunkNum, chunkNumTotal);
				}
				if (thriftResponse != null) {
					ByteBuffer payloadBuffer = charset.encode(CharBuffer.wrap(thriftResponse));
					syncObject.setPayload(payloadBuffer);
					responseBuffer = payloadProcessService.toByteBuffer(syncObject);
				}
			}
			else 
			{
				logger.error("Content-Length is bigger than 10MB ; " + req.getContentLength());
			}
		} catch (IOException e) {
			logger.error("IOError occurred", e);
		} catch (Throwable e) {
			logger.error("An error occured", e);
		}
		if (responseBuffer == null)
			responseBuffer = errorResponse;
		
		WritableByteChannel channel = Channels.newChannel(resp.getOutputStream());
        channel.write(responseBuffer);
        channel.close();
	}
}
