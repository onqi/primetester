package io.onqi.primetester.rest.resources;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

public class ErrorResource {
  private final Optional<String> error;

  public ErrorResource(String error) {
    this.error = Optional.ofNullable(error);
  }

  public Response buildResponse() {
    return Response.serverError().entity(this).type(MediaType.APPLICATION_JSON_TYPE).build();
  }
}
