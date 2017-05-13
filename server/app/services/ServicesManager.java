package services;

import models.LuisResponse;
import models.ResponseToClient;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.mvc.Result;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import utils.ConfigUtilities;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static play.mvc.Results.ok;

/**
 * This class contains instances of serviceProviders,
 * each of which provide one set of service.
 * e.g. luisServiceProvider for providing service from LUIS,
 * jiraServiceProvider for providing service from JIRA. etc.
 *
 * @see LuisServiceProvider
 * @see JiraReaderService
 */
public class ServicesManager {
  // These strings must comply with how they are defined in LUIS strictly.
  static final String LUIS_INTENT_ISSUE_DESCRIPTION = "IssueDescription";
  static final String LUIS_INTENT_ISSUE_ASSIGNEE = "IssueAssignee";
  static final String LUIS_INTENT_ISSUE_STATUS = "IssueStatus";
  static final String LUIS_INTENT_ISSUES_ON_STATUS = "IssuesForStatus";
  static final String LUIS_INTENT_ISSUES_OF_ASSIGNEE = "AssigneeIssues";
  static final String LUIS_INTENT_LIST_ALL_QUESTIONS = "AllQuestions";

  private LuisServiceProvider luisServiceProvider;
  private JiraServiceProvider jiraServiceProvider;
  private QuestionsDBServiceProvider questionsDBServiceProvider;

  @Inject
  public ServicesManager(WSClient ws) {
    this.luisServiceProvider = new LuisServiceProvider(ws);
    this.jiraServiceProvider = new JiraServiceProvider(ws);
    questionsDBServiceProvider = new QuestionsDBServiceProvider();
  }

  public CompletionStage<Result> interpretQueryAndActOnJira(String query) {
    try {
      //Add question to database
      questionsDBServiceProvider.addQuestion(query);
      LuisResponse luisResponse = luisServiceProvider.interpretQuery(query);

      // reading operations go here
      if (luisResponse.intent.equals(ServicesManager.LUIS_INTENT_ISSUE_DESCRIPTION) ||
        luisResponse.intent.equals(ServicesManager.LUIS_INTENT_ISSUE_ASSIGNEE) ||
        luisResponse.intent.equals(ServicesManager.LUIS_INTENT_ISSUE_STATUS)) {
        return jiraServiceProvider.readTicket(luisResponse.intent, luisResponse.entityName);
      } else if (luisResponse.intent.equals(ServicesManager.LUIS_INTENT_ISSUES_OF_ASSIGNEE)) {
        return jiraServiceProvider.readAssingeeInfo(luisResponse.intent, luisResponse.entityName);
      } else if (luisResponse.intent.equals(LUIS_INTENT_ISSUES_ON_STATUS)) {
        return jiraServiceProvider.readIssuesbyStatus(luisResponse.intent, luisResponse.entityName);
      } else if (luisResponse.intent.equals(ServicesManager.LUIS_INTENT_LIST_ALL_QUESTIONS)) {
        return CompletableFuture.supplyAsync(() -> ok(Json.toJson(
          new ResponseToClient(JiraServiceProvider.REQUEST_SUCCESS,
            questionsDBServiceProvider.getAllStoredQuestions()))
          )
        );
      } else {
        // TODO: updating operations go here
        throw new NotImplementedException();
      }
    } catch (Exception e) {
      // TODO: questions not understood go here
      return CompletableFuture.supplyAsync(() ->
        ok(Json.toJson(
          new ResponseToClient(JiraServiceProvider.REQUEST_FAILURE,
            ConfigUtilities.getString("error-message.luis-error")))
        )
      );
    }
  }
}
