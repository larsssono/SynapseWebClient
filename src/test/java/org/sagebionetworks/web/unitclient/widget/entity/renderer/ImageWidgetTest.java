package org.sagebionetworks.web.unitclient.widget.entity.renderer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.Page;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.widget.entity.registration.WidgetConstants;
import org.sagebionetworks.web.client.widget.entity.renderer.ImageWidget;
import org.sagebionetworks.web.client.widget.entity.renderer.ImageWidgetView;
import org.sagebionetworks.web.shared.EntityWrapper;
import org.sagebionetworks.web.test.helper.AsyncMockStubber;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class ImageWidgetTest {
		
	ImageWidget widget;
	ImageWidgetView mockView;
	SynapseClientAsync mockSynapseClient;
	NodeModelCreator mockNodeModelCreator;
	Page testPage;
	Map<String, String> descriptor;
	
	@Before
	public void setup() throws JSONObjectAdapterException{
		mockView = mock(ImageWidgetView.class);
		mockSynapseClient = mock(SynapseClientAsync.class);
		mockNodeModelCreator = mock(NodeModelCreator.class);
		AsyncMockStubber.callSuccessWith(new EntityWrapper()).when(mockSynapseClient).getEntity(anyString(), any(AsyncCallback.class));
		testPage = new Page();
		when(mockNodeModelCreator.createEntity(any(EntityWrapper.class))).thenReturn(testPage);
		widget = new ImageWidget(mockView, mockSynapseClient, mockNodeModelCreator);
		descriptor = new HashMap<String, String>();
		descriptor.put(WidgetConstants.IMAGE_WIDGET_FILE_NAME_KEY, "test name");
	}
	
	@Test
	public void testAsWidget() {
		widget.asWidget();
		verify(mockView).asWidget();
	}
	
	@Test
	public void testConfigure() {
		//set it up so that the requested entity really does have an attachment with that name
		List<AttachmentData> attachments = new ArrayList<AttachmentData>();
		AttachmentData testImage = new AttachmentData();
		testImage.setName("test name");
		testImage.setTokenId("test token");
		testImage.setMd5("test md5");
		attachments.add(testImage);
		testPage.setAttachments(attachments);
		
		widget.configure("", descriptor);
		verify(mockView).configure(anyString(), eq(testImage));
	}
	
	@Test
	public void testConfigureWhenEntityHasNullAttachments() {
		widget.configure("", descriptor);
		verify(mockView, times(0)).configure(anyString(), any(AttachmentData.class));
	}
	
	@Test
	public void testConfigureWhenEntityHasZeroAttachments() {
		testPage.setAttachments(new ArrayList());
		widget.configure("", descriptor);
		verify(mockView, times(0)).configure(anyString(), any(AttachmentData.class));
	}
	
	@Test
	public void testConfigureWhenEntityHasOtherAttachments() {
		List<AttachmentData> attachments = new ArrayList<AttachmentData>();
		AttachmentData testImage = new AttachmentData();
		testImage.setName("the wrong attachment");
		testImage.setTokenId("test token");
		testImage.setMd5("test md5");
		attachments.add(testImage);
		testPage.setAttachments(attachments);

		widget.configure("", descriptor);
		verify(mockView, times(0)).configure(anyString(), any(AttachmentData.class));
	}

}
