package daggerok;

import com.codeborne.selenide.WebDriverRunner;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootApplication
@EnableConfigurationProperties(TestApplicationProps.class)
class TestApplication { }

@Value
@ConstructorBinding
@ConfigurationProperties("test-application-props")
class TestApplicationProps {

  String baseUrl;
  User admin;
  User user;

  @Value // @ConstructorBinding
  static class User {
    String username;
    String password;
  }
}

@Log4j2
@Tag("e2e")
@AllArgsConstructor
@SpringBootTest(properties = {
    "spring.output.ansi.enabled=always",
    "test-application-props.user.username=user",
    "test-application-props.user.password=password",
    "test-application-props.admin.username=admin",
    "test-application-props.admin.password=admin",
    "test-application-props.base-url=http://127.0.0.1:8080",
})
class ApplicationTest {

  ApplicationContext context;

  @Test
  void unauthenticated_should_be_redirect_to_login_page() {
    var props = context.getBean(TestApplicationProps.class);
    open(String.format("%s/admin", props.getBaseUrl()));
    assertThat(WebDriverRunner.driver().url()).endsWith("/login");
  }

  @Test
  void user_should_not_be_authorized_to_admin_page() {
    var props = context.getBean(TestApplicationProps.class);
    open(String.format("%s/admin", props.getBaseUrl()));
    $("input[name='username']").setValue(props.getUser().getUsername());
    $("input[name='password']").setValue(props.getUser().getPassword()).submit();

    var body = $("body");
    log.info("body html: {}", body);
    body.shouldBe(exist, visible)
        .shouldHave(textCaseSensitive("403"))
        .shouldHave(textCaseSensitive("Forbidden"));
  }

  @Test
  void admin_should_be_authorized_to_admin_page() {
    var props = context.getBean(TestApplicationProps.class);
    open(String.format("%s/admin", props.getBaseUrl()));
    $("input[name='username']").setValue(props.getAdmin().getUsername());
    $("input[name='password']").setValue(props.getAdmin().getPassword()).submit();

    var h2 = $("h2");
    log.info("h2 html: {}", h2);
    h2.shouldBe(exist, visible)
      .shouldHave(text("administration"))
      .shouldHave(text("page"));
  }

  @Test
  void user_should_be_authorized_to_user_page() {
    var props = context.getBean(TestApplicationProps.class);
    open(String.format("%s/user", props.getBaseUrl()));
    $("input[name='username']").setValue(props.getUser().getUsername());
    $("input[name='password']").setValue(props.getUser().getPassword()).submit();

    var body = $("body");
    log.info("body: {}", body.text());
    body.shouldHave(textCaseSensitive("User"))
        .shouldHave(textCaseSensitive("page"));
  }

  @Test
  void admin_should_be_authorized_to_user_page() {
    var props = context.getBean(TestApplicationProps.class);
    open(String.format("%s/user", props.getBaseUrl()));
    $("input[name='username']").setValue(props.getAdmin().getUsername());
    $("input[name='password']").setValue(props.getAdmin().getPassword()).submit();

    var body = $("body");
    log.info("body: {}", body.text());
    body.shouldHave(textCaseSensitive("User"))
        .shouldHave(textCaseSensitive("page"));
  }

  @AfterEach
  void after() {
    closeWebDriver();
  }
}
