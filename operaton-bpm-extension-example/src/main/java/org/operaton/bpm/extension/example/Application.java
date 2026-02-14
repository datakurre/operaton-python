package org.operaton.bpm.extension.example;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.persistence.entity.GroupEntity;
import org.operaton.bpm.engine.impl.persistence.entity.UserEntity;
import org.operaton.bpm.spring.boot.starter.event.ProcessApplicationStartedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class Application {

  @Autowired private IdentityService identityService;

  public static void main(String... args) {
    SpringApplication.run(Application.class, args);
  }

  @EventListener
  public void handleProcessApplicationStartedEvent(
      ProcessApplicationStartedEvent processApplicationStartedEvent) {
    User demo = new UserEntity("demo");
    demo.setPassword("demo");
    identityService.saveUser(demo);

    Group admin = new GroupEntity("operaton-admin");
    identityService.saveGroup(admin);

    identityService.createMembership("demo", "operaton-admin");
  }
}
