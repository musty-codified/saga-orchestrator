package com.mustycodified.musty_process_payment.models;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import com.mustycodified.musty_process_payment.commonlib.exceptions.DataValidationException;
import com.mustycodified.musty_process_payment.commonlib.exceptions.SystemUnavailableException;
import com.mustycodified.musty_process_payment.commonlib.models.*;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangeTimedOutException;
import org.apache.camel.Processor;
import org.apache.camel.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import static com.mustycodified.musty_process_payment.commonlib.enums.ErrorCodesEnum.*;
import static com.mustycodified.musty_process_payment.commonlib.constants.ConstantsCommons.ORIGINAL_REQUEST;

@Component
public class ErrorHandlingProcessor implements Processor {

	private static final Logger logger = LoggerFactory.getLogger(ErrorHandlingProcessor.class);

	@Override
	public void process(Exchange exchange) throws Exception {

		Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);

		RequestWrapper originalRequest = exchange.getProperty(ORIGINAL_REQUEST, RequestWrapper.class);

		String routeCode = "";
		String routeName = "";
		String channelCode = "";
		String channelName = "";
		String messageID = "";
		String conversationID = "";

		ResponseWrapper responseWrapper = new ResponseWrapper();
		exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.INTERNAL_SERVER_ERROR.value());

		if (null != originalRequest) {

			responseWrapper.setResponsePayload(null);

			routeCode = originalRequest.getHeader().getRouteCode();
			routeName = originalRequest.getHeader().getRouteName();
			channelCode = originalRequest.getHeader().getChannelCode();
			channelName = originalRequest.getHeader().getChannelName();
			messageID = originalRequest.getHeader().getMessageID();
		}

		Item entry = null;
		List<Item> listOfItems = new ArrayList<>();

		EhfInfo ehfInfo = new EhfInfo();

		if (exception instanceof DataValidationException || exception instanceof UnrecognizedPropertyException
				|| exception instanceof ValidationException) {
			entry = new Item();
			entry.setEhfDesc(EHF1014.getMessage());
			entry.setEhfRef(EHF1014.getCode());
			listOfItems.add(entry);
			exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.BAD_REQUEST.value());
		} else if (exception instanceof SocketTimeoutException || exception instanceof SystemUnavailableException) {
			entry = new Item();
			entry.setEhfDesc(EHF1004.getMessage());
			entry.setEhfRef(EHF1004.getCode());
			listOfItems.add(entry);
			exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.BAD_REQUEST.value());
		} else {
			entry = new Item();
			entry.setEhfDesc(EHF1999.getMessage());
			entry.setEhfRef(EHF1999.getCode());
			listOfItems.add(entry);
		}

		ehfInfo.setItem(listOfItems);
		ResponseHeader header = new ResponseHeader();
		header.setEhfInfo(ehfInfo);

		header.setMessageID(messageID);
		header.setConversationID(conversationID);
		header.setTargetSystemID("NotAvailable");
		header.setChannelCode(channelCode);
		header.setChannelName(channelName);
		header.setRouteCode(routeCode);
		header.setRouteName(routeName);

		responseWrapper.setHeader(header);
		exchange.getIn().setBody(responseWrapper, ResponseWrapper.class);
		exchange.getIn().setHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

		// Logs all required information about the error
		logger.error("Adapter::Error::MessageID [{}]::Exception [{}]", messageID, exception.getMessage());

	}
}
