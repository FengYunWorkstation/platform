/***************************************************************************
 * Copyright 2001-2003 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.webui.component.lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.component.UIApplication;
import org.exoplatform.webui.component.UIComponent;
import org.exoplatform.webui.component.UIForm;
import org.exoplatform.webui.component.UIFormInput;
import org.exoplatform.webui.component.UIFormInputBase;
import org.exoplatform.webui.component.UIFormInputSet;
import org.exoplatform.webui.component.UIPortletApplication;
import org.exoplatform.webui.component.validator.Validator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.exception.MessageException;
/**
 * Author : Nhu Dinh Thuan
 *          nhudinhthuan@yahoo.com
 * Jun 1, 2006
 */
public class UIFormLifecycle  extends Lifecycle {

  public void processDecode(UIComponent uicomponent, WebuiRequestContext context) throws Exception {
    UIForm uiForm = (UIForm) uicomponent ;
//    HttpServletRequest httpRequest = (HttpServletRequest)context.getRequest() ;
    uiForm.setSubmitAction(null) ;
//    if(ServletFileUpload.isMultipartContent(new ServletRequestContext(httpRequest))) {
//      processMultipartRequest(uiForm, context) ;
//    } else {
    processNormalRequest(uiForm, context) ;
//    }
    List<UIComponent>  children =  uiForm.getChildren() ;
    for(UIComponent uiChild :  children) {
      uiChild.processDecode(context) ;     
    }
    String action =  uiForm.getSubmitAction(); 
    Event<UIComponent> event = uicomponent.createEvent(action, Event.Phase.DECODE, context) ;
    if(event != null)  event.broadcast() ;
  }

  public void processAction(UIComponent uicomponent , WebuiRequestContext context) throws Exception {
    UIForm uiForm = (UIForm) uicomponent ;
    String action =  context.getRequestParameter(UIForm.ACTION);
    if(action == null) action = uiForm.getSubmitAction();
    if(action == null) return ;    
    Event<UIComponent> event = uicomponent.createEvent(action, Event.Phase.PROCESS, context) ;
    if(event == null) {
      event = uicomponent.<UIComponent>getParent().createEvent(action, Event.Phase.PROCESS, context) ;
    }
    if(event == null) return;
    
    UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
    List<UIComponent>  children = uiForm.getChildren() ;
    validateChildren(children, uiApp, context);
    /*List<Validator> validators = uiForm.getValidators() ;
    if(validators != null) {
      try {
        for(Validator validator : validators) validator.validate(uiForm) ;
      } catch (MessageException ex) {
        uiApp.addMessage(ex.getDetailMessage()) ;
        context.setProcessRender(true) ;
      } catch(Exception ex) {
        //TODO:  This is a  critical exception and should be handle  in the UIApplication
        uiApp.addMessage(new ApplicationMessage(ex.toString(), null)) ;        
        context.setProcessRender(true) ;
      }
    }*/
    if(context.getProcessRender()) {
      if(uiApp instanceof UIPortletApplication){
        context.addUIComponentToUpdateByAjax(uiApp);
      }else{
        context.addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
      }
      return ;     
    }
    event.broadcast()  ;
  }
  
  private void processNormalRequest(UIForm uiForm, WebuiRequestContext context) throws Exception {
    List<UIFormInputBase> inputs = new ArrayList<UIFormInputBase>() ;
    uiForm.findComponentOfType(inputs, UIFormInputBase.class) ;
    uiForm.setSubmitAction(context.getRequestParameter(UIForm.ACTION)) ;
    for(UIFormInputBase input :  inputs) {
      String inputValue = context.getRequestParameter(input.getId()) ;
      if(inputValue == null || inputValue.trim().length() == 0){
        inputValue = context.getRequestParameter(input.getName()) ;
      }
      input.decode(inputValue, context);
    }    
  }
  
//  private void processMultipartRequest(UIForm uiForm, RequestContext context) throws Exception {
//    HttpServletRequest httpRequest = (HttpServletRequest)context.getRequest() ;
//    ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
//    List items = upload.parseRequest(httpRequest);
//    Iterator iter = items.iterator();
//    while (iter.hasNext()) {
//      FileItem item = (FileItem) iter.next();
//      String fieldName = item.getFieldName();      
//      if (item.isFormField()) {  //Normal  inputs
//        String inputValue = item.getString() ;
//        if (UIForm.ACTION.equals(fieldName)) {
//          uiForm.setSubmitAction(inputValue) ;
//          continue;
//        } else if(UIFormTabPane.RENDER_TAB.equals(fieldName)){
//          ((UIFormTabPane)uiForm).setRenderTabId(inputValue);
//          continue;
//        }
//        UIFormInputBase input =  uiForm.findComponentById(fieldName) ;
//        if(input != null) input.decode(inputValue, context) ;
//        continue;
//      }
//      UIFormInputBase input =  uiForm.findComponentById(fieldName) ;  // File input
//      if(input != null) input.decode(item, context) ;
//    }
//    
//  }
  
  @SuppressWarnings("unchecked")
  private void validateChildren(List<UIComponent>  children, UIApplication uiApp, WebuiRequestContext context) {
    for(UIComponent uiChild : children) {
      if(uiChild instanceof UIFormInput) {
        UIFormInput uiInput =  (UIFormInput) uiChild ;
        List<Validator> validators = uiInput.getValidators() ;
        if(validators == null) continue;
        try {
          for(Validator validator : validators) validator.validate(uiInput) ;
        } catch (MessageException ex) {
          uiApp.addMessage(ex.getDetailMessage()) ;
          context.setProcessRender(true) ;
        } catch(Exception ex) {
          //TODO:  This is a  critical exception and should be handle  in the UIApplication
          uiApp.addMessage(new ApplicationMessage(ex.getMessage(), null)) ;
          context.setProcessRender(true) ;
        }
      } else if(uiChild instanceof UIFormInputSet){
        UIFormInputSet uiInputSet = (UIFormInputSet)uiChild;
        validateChildren(uiInputSet.getChildren(), uiApp, context);
      }
    }
  }
}