package io.onqi.primetester.rest;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import io.onqi.primetester.Application;
import io.onqi.primetester.rest.resources.CreateTaskResource;
import io.onqi.primetester.rest.resources.ResultResource;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.RedirectionException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class TasksEndpointTest extends JerseyTest {
  @Override
  protected javax.ws.rs.core.Application configure() {
    return new Application();
  }

  @Override
  protected void configureClient(ClientConfig config) {
    config.register(new JacksonJsonProvider(Application.OBJECT_MAPPER))
            .property(ClientProperties.FOLLOW_REDIRECTS, false);
  }

  @Test
  @Ignore("TODO figure out why ClientResponse is null here")
  public void testNewTaskCreation() {
    ClientResponse response = target("tasks").request()
            .post(Entity.entity(new CreateTaskResource("1234"), MediaType.APPLICATION_JSON_TYPE), ClientResponse.class);

    assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED);
    assertThat(response.getHeaderString("Location")).contains("tasks/1");

  }

  @Test
  public void testStatusQuery() {
    target("tasks").request()
            .post(Entity.entity(new CreateTaskResource("29927402397991286489627837734179186385188296382227"),
                    MediaType.APPLICATION_JSON_TYPE), ClientResponse.class);

    TaskStatusResource response = target("tasks").path("1").request().get(TaskStatusResource.class);
    assertThat(response.getTaskId()).isEqualTo(1);
    assertThat(response.getStatus()).isIn(TaskStatusResource.Status.values());
  }

  @Test
  public void testRedirectToResults() {
    String number = "1";
    target("tasks").request()
            .post(Entity.entity(new CreateTaskResource(number),
                    MediaType.APPLICATION_JSON_TYPE), ClientResponse.class);
    try {
      target("tasks").path(number).request().get(ClientResponse.class);
    } catch (RedirectionException e) {
      assertThat(e.getResponse().getStatus()).isEqualTo(Response.Status.SEE_OTHER.getStatusCode());
      URI resultsLocation = e.getLocation();
      ResultResource resultResource = target().path(resultsLocation.getPath()).request().get(ResultResource.class);
      assertThat(resultResource).isNotNull();
      assertThat(resultResource.getNumber()).isEqualTo(number);
      assertThat(resultResource.getDivider()).isEqualTo(Optional.empty());
    }
  }
}
