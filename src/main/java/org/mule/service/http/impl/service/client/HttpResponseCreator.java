/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.impl.service.client;

import static java.lang.Long.parseLong;
import static org.mule.runtime.api.metadata.MediaType.MULTIPART_MIXED;
import static org.mule.runtime.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.runtime.http.api.HttpHeaders.Names.CONTENT_TYPE;
import org.mule.runtime.http.api.domain.entity.EmptyHttpEntity;
import org.mule.runtime.http.api.domain.entity.HttpEntity;
import org.mule.runtime.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.runtime.http.api.domain.message.response.HttpResponseBuilder;
import org.mule.service.http.impl.service.domain.entity.multipart.StreamedMultipartHttpEntity;

import com.ning.http.client.Response;

import java.io.IOException;
import java.io.InputStream;

/**
 * Converts {@link Response Responses} to {@link HttpResponse HttpResponses}.
 *
 * @since 1.0
 */
public class HttpResponseCreator {

  public HttpResponse create(Response response, InputStream inputStream) throws IOException {
    HttpResponseBuilder responseBuilder = HttpResponse.builder();
    responseBuilder.statusCode(response.getStatusCode());
    responseBuilder.reasonPhrase(response.getStatusText());
    String contentType = response.getHeader(CONTENT_TYPE.toLowerCase());
    String contentLength = response.getHeader(CONTENT_LENGTH.toLowerCase());
    responseBuilder.entity(createEntity(inputStream, contentType, contentLength));

    if (response.hasResponseHeaders()) {
      for (String header : response.getHeaders().keySet()) {
        for (String headerValue : response.getHeaders(header)) {
          responseBuilder.addHeader(header, headerValue);
        }
      }
    }
    return responseBuilder.build();
  }

  private HttpEntity createEntity(InputStream stream, String contentType, String contentLength) {
    Long contentLengthAsLong = -1L;
    if (contentLength != null) {
      contentLengthAsLong = parseLong(contentLength);
    }
    if (contentType != null && contentType.startsWith(MULTIPART_MIXED.getPrimaryType())) {
      if (contentLengthAsLong >= 0) {
        return new StreamedMultipartHttpEntity(stream, contentType, contentLengthAsLong);
      } else {
        return new StreamedMultipartHttpEntity(stream, contentType);
      }
    } else {
      if (contentLengthAsLong > 0) {
        return new InputStreamHttpEntity(stream, contentLengthAsLong);
      } else if (contentLengthAsLong == 0) {
        return new EmptyHttpEntity();
      } else {
        return new InputStreamHttpEntity(stream);
      }
    }
  }

}
