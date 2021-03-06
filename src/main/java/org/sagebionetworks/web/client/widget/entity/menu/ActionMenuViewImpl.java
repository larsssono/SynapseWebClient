package org.sagebionetworks.web.client.widget.entity.menu;

import java.util.List;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Link;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.DisplayUtils.IconSize;
import org.sagebionetworks.web.client.EntityTypeProvider;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.SynapseJSNIUtils;
import org.sagebionetworks.web.client.events.CancelEvent;
import org.sagebionetworks.web.client.events.CancelHandler;
import org.sagebionetworks.web.client.events.EntityUpdatedEvent;
import org.sagebionetworks.web.client.events.EntityUpdatedHandler;
import org.sagebionetworks.web.client.model.EntityBundle;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.utils.TOOLTIP_POSITION;
import org.sagebionetworks.web.client.widget.entity.browse.EntityTreeBrowser;
import org.sagebionetworks.web.client.widget.entity.browse.MyEntitiesBrowser;
import org.sagebionetworks.web.client.widget.entity.browse.MyEntitiesBrowser.SelectedHandler;
import org.sagebionetworks.web.client.widget.entity.download.LocationableUploader;
import org.sagebionetworks.web.client.widget.sharing.AccessControlListEditor;
import org.sagebionetworks.web.client.widget.sharing.AccessMenuButton;
import org.sagebionetworks.web.shared.EntityType;
import org.sagebionetworks.web.shared.EntityWrapper;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.HorizontalPanel;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FitData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ActionMenuViewImpl extends HorizontalPanel implements ActionMenuView {

	private Presenter presenter;
	private IconsImageBundle iconsImageBundle;
	private AccessControlListEditor accessControlListEditor;
	private LocationableUploader locationableUploader;
	private MyEntitiesBrowser myEntitiesBrowser;
	private EntityTypeProvider typeProvider;
	private SynapseJSNIUtils synapseJSNIUtils;
	private boolean readOnly;
	
	private Button editButton;
	private Button shareButton;
	private Button addButton;
	private Button toolsButton;
	private Button deleteButton;
	
	@Inject
	public ActionMenuViewImpl(SageImageBundle sageImageBundle,
			IconsImageBundle iconsImageBundle, 
			AccessMenuButton accessMenuButton,
			AccessControlListEditor accessControlListEditor,
			LocationableUploader locationableUploader, 
			MyEntitiesBrowser myEntitiesBrowser, 
			EntityTypeProvider typeProvider,
			SynapseJSNIUtils synapseJSNIUtils) {
		this.iconsImageBundle = iconsImageBundle;
		this.accessControlListEditor = accessControlListEditor;
		this.locationableUploader = locationableUploader;
		this.myEntitiesBrowser = myEntitiesBrowser;
		this.typeProvider = typeProvider;
		this.synapseJSNIUtils = synapseJSNIUtils;
//		this.setLayout(new FitLayout());
		this.setHorizontalAlign(HorizontalAlignment.RIGHT);
		this.setTableWidth("100%");
	}

	@Override
	public void createMenu(
			EntityBundle entityBundle, 
			EntityType entityType, 
			AuthenticationController authenticationController,
			boolean isAdministrator,
			boolean canEdit, 
			boolean readOnly) {
		this.readOnly = readOnly;
		Entity entity = entityBundle.getEntity();

		UserSessionData sessionData = authenticationController.getLoggedInUser();
		UserProfile userProfile = (sessionData==null ? null : sessionData.getProfile());

		// edit button
		if(editButton == null) {			
			editButton = new Button(DisplayConstants.BUTTON_EDIT, AbstractImagePrototype.create(iconsImageBundle.editGrey16()));
			editButton.setId(DisplayConstants.ID_BTN_EDIT);
			editButton.setHeight(25);
			this.add(editButton);
			this.add(new HTML(SafeHtmlUtils.fromSafeConstant("&nbsp;")));			
		}				
		if (canEdit && !readOnly) editButton.enable();
		else editButton.disable();
		configureEditButton(entity, entityType);	
		
		// share button
		if(shareButton == null) { 
			shareButton = new Button(DisplayConstants.BUTTON_SHARE, AbstractImagePrototype.create(iconsImageBundle.mailGrey16()));
			shareButton.setId(DisplayConstants.ID_BTN_SHARE);
			shareButton.setHeight(25);
			this.add(shareButton);
			this.add(new HTML(SafeHtmlUtils.fromSafeConstant("&nbsp;")));
		}
		if (isAdministrator && !readOnly) shareButton.enable();
		else shareButton.disable();
		configureShareButton(entity);		

		// add Button
		if(addButton == null) {
			addButton = new Button(DisplayConstants.BUTTON_ADD, AbstractImagePrototype.create(iconsImageBundle.add16()));
			addButton.setId(DisplayConstants.ID_BTN_ADD);
			addButton.setHeight(25);
			this.add(addButton);
			this.add(new HTML(SafeHtmlUtils.fromSafeConstant("&nbsp;")));
		}
		if (canEdit && !readOnly) addButton.enable();
		else addButton.disable();
		configureAddMenu(entity, entityType);

		if(toolsButton == null) {
			toolsButton = new Button(DisplayConstants.BUTTON_TOOLS_MENU, AbstractImagePrototype.create(iconsImageBundle.adminToolsGrey16()));
			toolsButton.setHeight(25);
			this.add(toolsButton);	
		}
		
		if(deleteButton == null) {
			deleteButton = getDeleteButton(entityType);
			this.add(deleteButton);
		}
		
		if (isAdministrator)
			deleteButton.enable();
		else deleteButton.disable();
		configureDeleteButton(entityType);
		
		configureToolsMenu(entityBundle, entityType, isAdministrator, canEdit);
	}
	
	@Override
	public Widget asWidget() {
		return this;
	}	

	@Override 
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}
		
	@Override
	public void showErrorMessage(String message) {
		DisplayUtils.showErrorMessage(message);
	}

	@Override
	public void showLoading() {
	}

	@Override
	public void showInfo(String title, String message) {
		DisplayUtils.showInfo(title, message);
	}

	@Override
	public void clear() {
		if(editButton != null) editButton.removeAllListeners();
		if(shareButton != null) shareButton.removeAllListeners();	
	}
	
	/*
	 * Private Methods
	 */
	private void configureEditButton(final Entity entity, EntityType entityType) {
		editButton.removeAllListeners();
		editButton.addSelectionListener(new SelectionListener<ButtonEvent>() {

			@Override
			public void componentSelected(ButtonEvent ce) {
				// the presenter should handle this
				presenter.onEdit();
			}
		});		
	}

	private void configureDeleteButton(EntityType entityType) {
		final String typeDisplay = typeProvider.getEntityDispalyName(entityType);
		deleteButton.removeAllListeners();
		deleteButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				handleDeleteClick(typeDisplay);
			}
		});
	}
	private void configureShareButton(Entity entity) {		
		accessControlListEditor.setResource(entity);
		shareButton.removeAllListeners();		
		shareButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				final Dialog window = new Dialog();
				
				// configure layout
				window.setSize(560, 465);
				window.setPlain(true);
				window.setModal(true);
				window.setBlinkModal(true);
				window.setHeading(DisplayConstants.TITLE_SHARING_PANEL);
				window.setLayout(new FitLayout());
				window.add(accessControlListEditor.asWidget(), new FitData(4));			    
			    
				// configure buttons
				window.okText = "Save";
				window.cancelText = "Cancel";
			    window.setButtons(Dialog.OKCANCEL);
			    window.setButtonAlign(HorizontalAlignment.RIGHT);
			    window.setHideOnButtonClick(false);
				window.setResizable(false);
				
				// "Apply" button
				// TODO: Disable the "Apply" button if ACLEditor has no unsaved changes
				Button applyButton = window.getButtonById(Dialog.OK);
				applyButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
					@Override
					public void componentSelected(ButtonEvent ce) {
						// confirm close action if there are unsaved changes
						if (accessControlListEditor.hasUnsavedChanges()) {
							accessControlListEditor.pushChangesToSynapse(false, new AsyncCallback<EntityWrapper>() {
								@Override
								public void onSuccess(EntityWrapper result) {
									presenter.fireEntityUpdatedEvent();
								}
								@Override
								public void onFailure(Throwable caught) {
									//failure notification is handled by the acl editor view.
								}
							});
						}
						window.hide();
					}
			    });
				
				// "Close" button				
				Button closeButton = window.getButtonById(Dialog.CANCEL);
			    closeButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
					@Override
					public void componentSelected(ButtonEvent ce) {
						window.hide();
					}
			    });
				
				window.show();
			}
		});		
	}
	
	private void configureAddMenu(final Entity entity, final EntityType entityType) {		
		// create add menu button from children
		Menu menu = new Menu();
		
		List<EntityType> children = entityType.getValidChildTypes();
		List<EntityType> skipTypes = presenter.getAddSkipTypes();
		if(children != null) {			 
			// add child tabs in order
			for(EntityType child : DisplayUtils.orderForDisplay(children)) {
				if(skipTypes.contains(child)) continue; // skip some types
				menu.add(createAddMenuItem(child, entity));
			}
		}
			
		if(menu.getItemCount() == 0) {
			addButton.disable();
		}
		addButton.setMenu(menu);
	}

	private MenuItem createAddMenuItem(final EntityType childType, final Entity entity) {
		String displayName = typeProvider.getEntityDispalyName(childType);			
		MenuItem item = new MenuItem(displayName);				
		item.setIcon(AbstractImagePrototype.create(DisplayUtils
				.getSynapseIconForEntityType(childType, IconSize.PX16,
						iconsImageBundle)));				
		item.addSelectionListener(new SelectionListener<MenuEvent>() {
			public void componentSelected(MenuEvent menuEvent) {
				presenter.addNewChild(childType, entity.getId());
			}
		});
		return item;
	}
	
	private void configureToolsMenu(EntityBundle entityBundle, EntityType entityType, boolean isAdministrator, boolean canEdit) {
		toolsButton.enable();
		
		boolean authenticated = presenter.isUserLoggedIn();
		// disable edit/admin items if in read-only mode
		canEdit &= !readOnly;
		isAdministrator &= !readOnly;
		
		// create drop down menu
		Menu menu = new Menu();		
		
		Entity entity = entityBundle.getEntity();
		
		// upload
		if(canEdit) {
			addUploadItem(menu, entityBundle, entityType);
		}
		// create link
		if(authenticated) {
			addCreateShortcutItem(menu, entity, entityType);
		}
		// move
		if (canEdit) {
			addMoveItem(menu, entity, entityType);
		}

		toolsButton.setMenu(menu);
		if(menu.getItemCount() == 0) {
			toolsButton.disable();
		}
	}

	/**
	 * 'Delete Entity' item
	 * @param entityType 
	 */
	private Button getDeleteButton(EntityType entityType) {
		Button deleteButton = new Button("", AbstractImagePrototype.create(iconsImageBundle.delete16()));
		deleteButton.setHeight(25);
		DisplayUtils.addTooltip(synapseJSNIUtils, deleteButton, DisplayConstants.LABEL_DELETE, TOOLTIP_POSITION.BOTTOM);
		return deleteButton;
	}
	
	private void handleDeleteClick(final String typeDisplay) {
		MessageBox.confirm(DisplayConstants.LABEL_DELETE +" " + typeDisplay, DisplayConstants.PROMPT_SURE_DELETE + " " + typeDisplay +"?", new Listener<MessageBoxEvent>() {					
			@Override
			public void handleEvent(MessageBoxEvent be) { 					
				Button btn = be.getButtonClicked();
				if(Dialog.YES.equals(btn.getItemId())) {
					presenter.deleteEntity();
				}
			}
		});
	}
	
	/**
	 * 'Upload File' item
	 * @param menu
	 * @param entity 
	 * @param entityType 
	 */
	private void addUploadItem(Menu menu, final EntityBundle entityBundle, EntityType entityType) {
		if(entityBundle.getEntity() instanceof Locationable) {
			MenuItem item = new MenuItem(DisplayConstants.TEXT_UPLOAD_FILE);
			item.setIcon(AbstractImagePrototype.create(iconsImageBundle.NavigateUp16()));
			final Window window = new Window();  
			locationableUploader.clearHandlers();
			locationableUploader.addPersistSuccessHandler(new EntityUpdatedHandler() {				
				@Override
				public void onPersistSuccess(EntityUpdatedEvent event) {
					window.hide();
					presenter.fireEntityUpdatedEvent();
				}
			});
			locationableUploader.addCancelHandler(new CancelHandler() {				
				@Override
				public void onCancel(CancelEvent event) {
					window.hide();
				}
			});
			item.addSelectionListener(new SelectionListener<MenuEvent>() {
				@Override
				public void componentSelected(MenuEvent ce) {
					window.removeAll();
					window.setSize(400, 320);
					window.setPlain(true);
					window.setModal(true);		
					window.setBlinkModal(true);
					window.setHeading(DisplayConstants.TEXT_UPLOAD_FILE);
					window.setLayout(new FitLayout());			
					window.add(locationableUploader.asWidget(entityBundle.getEntity(), entityBundle.getAccessRequirements()), new MarginData(5));
					window.show();
				}
			});			
			menu.add(item);
		}
	}
		
	/**
	 * 'Create Link' item
	 * @param menu
	 * @param entity 
	 * @param entityType 
	 */
	private void addCreateShortcutItem(Menu menu, Entity entity,EntityType entityType) {	
		// Create shortcut
		MenuItem item = new MenuItem(DisplayConstants.LABEL_CREATE_LINK);
		item.setIcon(AbstractImagePrototype.create(DisplayUtils.getSynapseIconForEntityClassName(Link.class.getName(), IconSize.PX16, iconsImageBundle)));		
		item.addSelectionListener(new SelectionListener<MenuEvent>() {
			@Override
			public void componentSelected(MenuEvent ce) {				
				final Window window = new Window();  
	
				EntityTreeBrowser tree = myEntitiesBrowser.getEntityTreeBrowser();
				tree.setMakeLinks(false);
				tree.setShowContextMenu(false);
				myEntitiesBrowser.setEntitySelectedHandler(new SelectedHandler() {					
					@Override
					public void onSelection(String selectedEntityId) {
						presenter.createLink(selectedEntityId);
						window.hide();
					}
				});
				
				window.setSize(483, 329);
				window.setPlain(true);
				window.setModal(true);
				window.setBlinkModal(true);
				window.setHeading(DisplayConstants.LABEL_WHERE_SAVE_LINK);
				window.setLayout(new FitLayout());
				window.add(myEntitiesBrowser.asWidget(), new FitData(4)); 				
				window.addButton(new Button(DisplayConstants.BUTTON_CANCEL, new SelectionListener<ButtonEvent>() {
					@Override
					public void componentSelected(ButtonEvent ce) {
						window.hide();
					}
				}));
				window.setButtonAlign(HorizontalAlignment.CENTER);
				window.show();
	
			}
		});
		menu.add(item);
	}

	/**
	 * 'Move Entity' item
	 * @param menu
	 * @param entity 
	 * @param entityType 
	 */
	private void addMoveItem(Menu menu, final Entity entity, EntityType entityType) {
		final String typeDisplay = typeProvider.getEntityDispalyName(entityType);
		MenuItem itemMove = new MenuItem(DisplayConstants.LABEL_MOVE + " " + typeDisplay);
		itemMove.setIcon(AbstractImagePrototype.create(iconsImageBundle.moveButton16()));		
		itemMove.addSelectionListener(new SelectionListener<MenuEvent>() {
			@Override
			public void componentSelected(MenuEvent ce) {				
				final Window window = new Window();  
	
				EntityTreeBrowser tree = myEntitiesBrowser.getEntityTreeBrowser();
				tree.setMakeLinks(false);
				tree.setShowContextMenu(false);
				myEntitiesBrowser.setEntitySelectedHandler(new SelectedHandler() {					
					@Override
					public void onSelection(String selectedEntityId) {
						presenter.moveEntity(selectedEntityId);
						window.hide();
					}
				});
				
				window.setSize(483, 329);
				window.setPlain(true);
				window.setModal(true);
				window.setBlinkModal(true);
				window.setHeading(DisplayConstants.LABEL_MOVE + " " + typeDisplay);
				window.setLayout(new FitLayout());
				window.add(myEntitiesBrowser.asWidget(), new FitData(4)); 				
				window.addButton(new Button(DisplayConstants.BUTTON_CANCEL, new SelectionListener<ButtonEvent>() {
					@Override
					public void componentSelected(ButtonEvent ce) {
						window.hide();
					}
				}));
				window.setButtonAlign(HorizontalAlignment.CENTER);
				window.show();
	
			}
		});
		menu.add(itemMove);
	}
}
