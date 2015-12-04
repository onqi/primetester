package io.onqi.primetester.web.rest;

import io.onqi.primetester.web.rest.resources.PrimeTestResource;

import java.net.URI;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/prime-test")
class PrimeTestEndpoint {

  @POST
  public Response postNumber(@Valid PrimeTestResource resource) {
    return Response.created(URI.create("/prime-test/1234")).build();
  }
}
