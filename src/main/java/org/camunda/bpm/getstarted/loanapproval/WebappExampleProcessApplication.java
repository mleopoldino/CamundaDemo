package org.camunda.bpm.getstarted.loanapproval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.Tenant;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.camunda.bpm.spring.boot.starter.event.PostDeployEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;

import br.com.interfile.interflow.core.commons.util.BpmUtil;

@SpringBootApplication
@EnableProcessApplication
public class WebappExampleProcessApplication {

	public static void main(String... args) {
		SpringApplication.run(WebappExampleProcessApplication.class, args);
	}

	@Autowired
	private RuntimeService runtimeService;

	@EventListener
	private void processPostDeploy(PostDeployEvent event) {
		runtimeService.startProcessInstanceByKey("loanApproval");
		
		final IdentityService identityService = event.getProcessEngine().getIdentityService();

		if (identityService.isReadOnly()) {
			System.out.println("Identity service provider is Read Only, not creating users.");
			return;
		}

		User admin = identityService.createUserQuery().userId("admin").singleResult();

		Tenant tenant = identityService.createTenantQuery().tenantId("LoanApproval").singleResult();
		tenant = BpmUtil.createTenant(event.getProcessEngine(), "LoanApproval","loan-approval");
		
		// Criação de Usuários
		User analista = BpmUtil.createUser(event.getProcessEngine(), "analista", "Analista", "Instituição", "1234", admin.getEmail(), tenant);
		User supervisor = BpmUtil.createUser(event.getProcessEngine(), "supervisor", "Supervisor", "Instituição", "1234", admin.getEmail(), tenant);
		// Criação de Grupos
		Group Equipe_Analista = BpmUtil.createGroup(event.getProcessEngine(), "analistaGroupId", "Analistas", "WORKFLOW", tenant);
		Group Equipe_Supervisor = BpmUtil.createGroup(event.getProcessEngine(), "supervisorGroupId", "Supervisores", "WORKFLOW", tenant);
		// Definição de Grupos Membros
		BpmUtil.createMembership(event.getProcessEngine(), analista, Equipe_Analista);
		BpmUtil.createMembership(event.getProcessEngine(), supervisor, Equipe_Supervisor);
		// Autorização de Acesso a Tasklist
		BpmUtil.createTaskListAuthorization(event.getProcessEngine(), Equipe_Analista);
		BpmUtil.createTaskListAuthorization(event.getProcessEngine(), Equipe_Supervisor);
		// Autorização de Visualização ao Processo
		BpmUtil.createStartProcessAuthorization(event.getProcessEngine(), Equipe_Analista, "loanApproval");
		BpmUtil.createStartProcessAuthorization(event.getProcessEngine(), Equipe_Supervisor, "loanApproval");
		// Acesso a filtros
		BpmUtil.createFilter(event.getProcessEngine(), Equipe_Analista, 5, getVariables());
		BpmUtil.createFilter(event.getProcessEngine(), Equipe_Supervisor, 10, getVariables());
	}
	
	public static List<Object> getVariables() {
		List<Object> variables = new ArrayList<Object>();

		variables.add(getVariable("cotacao", "Cotação"));
		variables.add(getVariable("revision", "Revisão"));

		return variables;
	}
	
	private static Object getVariable(String name, String label) {
		Map<String, String> variable = new HashMap<String, String>();
		variable.put("name", name);
		variable.put("label", label);
		return variable;
	}
	
}
