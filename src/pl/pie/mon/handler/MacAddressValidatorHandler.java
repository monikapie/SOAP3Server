package pl.pie.mon.handler;

import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPFaultException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MacAddressValidatorHandler implements SOAPHandler<SOAPMessageContext>{
    @Override
    public Set<QName> getHeaders() {
        System.out.println("Server: getHeaders()...");

        return null;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        System.out.println("Server: handleMessage()...");
        Boolean isRequest = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        Map http_headers = (Map) context.get(MessageContext.HTTP_REQUEST_HEADERS);

        if(!isRequest){
            try {
                SOAPMessage soapMessage = context.getMessage();
                SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
                SOAPHeader soapHeader = soapEnvelope.getHeader();
                if(soapHeader == null){
                    soapHeader = soapEnvelope.addHeader();
                    generateSOAPErrMessage(soapMessage, "No header.");
                }

                Iterator it = soapHeader.extractHeaderElements(SOAPConstants.URI_SOAP_ACTOR_NEXT);

                if(it == null || !it.hasNext()){
                    generateSOAPErrMessage(soapMessage, "No header block for actor.");
                }

                Node macNode = (Node) it.next();
                String macValue = (macNode == null) ? null : macNode.getValue();

                if(macNode == null){
                    generateSOAPErrMessage(soapMessage, "No MAC address.");
                }

                if(!macValue.equals("5C-26-0A-57-78-CC")){
                    generateSOAPErrMessage(soapMessage, "Invalid MAC address, access is denied.");
                }

                validateUserCredentials(http_headers, soapMessage);

                soapMessage.writeTo(System.out);

            } catch (SOAPException | IOException e) {
                System.err.println(e);
            }

        }

        return true;
    }

    private void validateUserCredentials(Map http_headers, SOAPMessage soapMessage) {
        List userList = (List) http_headers.get("Username");
        List pwdList = (List) http_headers.get("Password");
        String username = "";
        String userPass = "";

        if(userList != null){
            username = userList.get(0).toString();
        }

        if(pwdList != null){
            userPass = pwdList.get(0).toString();
        }

        if (!(username.equals("monika") && userPass.equals("pie"))){
            generateSOAPErrMessage(soapMessage, "Invalid credentials.");
        }
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        System.out.println("Server: handleFault()...");

        return true;
    }

    @Override
    public void close(MessageContext context) {
        System.out.println("Server: close()...");

    }

    private void generateSOAPErrMessage(SOAPMessage msg, String reason){
        SOAPBody soapBody = null;
        try {
            soapBody = msg.getSOAPPart().getEnvelope().getBody();
            SOAPFault soapFault = soapBody.addFault();
            soapFault.setFaultString(reason);
            throw new SOAPFaultException(soapFault);
        } catch (SOAPException e) { }
    }
}
