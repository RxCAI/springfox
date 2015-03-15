package springdox.documentation.spring.web.readers

import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import springdox.documentation.builders.OperationBuilder
import springdox.documentation.service.Operation
import springdox.documentation.spi.service.contexts.AuthorizationContext
import springdox.documentation.spi.service.contexts.RequestMappingContext
import springdox.documentation.spring.web.mixins.AuthSupport
import springdox.documentation.spring.web.mixins.RequestMappingSupport
import springdox.documentation.spring.web.mixins.ServicePluginsSupport
import springdox.documentation.spring.web.plugins.DocumentationContextSpec
import springdox.documentation.spring.web.plugins.DocumentationPluginsManager
import springdox.documentation.spring.web.readers.operation.ApiOperationReader
import springdox.documentation.spring.web.readers.operation.DefaultOperationBuilder
import springdox.documentation.spring.web.scanners.RegexRequestMappingPatternMatcher

import static com.google.common.collect.Sets.*
import static org.springframework.web.bind.annotation.RequestMethod.*

@Mixin([RequestMappingSupport, AuthSupport, ServicePluginsSupport])
class ApiOperationReaderSpec extends DocumentationContextSpec {
  ApiOperationReader sut

  def setup() {
    AuthorizationContext authorizationContext = AuthorizationContext.builder()
            .withAuthorizations(defaultAuth())
            .withRequestMappingPatternMatcher(new RegexRequestMappingPatternMatcher())
            .withIncludePatterns(newHashSet(".*"))
            .build()
    plugin.authorizationContext(authorizationContext)
    sut = new ApiOperationReader(customWebPlugins([],[],[new DefaultOperationBuilder()]))
  }

  def "Should generate default operation on handler method without swagger annotations"() {

    given:
      RequestMappingInfo requestMappingInfo = requestMappingInfo("/doesNotMatterForThisTest",
              [
                      patternsRequestCondition      : patternsRequestCondition('/doesNotMatterForThisTest', '/somePath/{businessId:\\d+}'),
                      requestMethodsRequestCondition: requestMethodsRequestCondition(PATCH, POST)
              ]
      )

      HandlerMethod handlerMethod = dummyHandlerMethod()

      RequestMappingContext context = new RequestMappingContext(context(),
              requestMappingInfo,
              handlerMethod)
    when:
      def operations = sut.read(context)

    then:
      Operation apiOperation = operations[0]
      apiOperation.getMethod() == PATCH.toString()
      apiOperation.getSummary() == handlerMethod.method.name
      apiOperation.getNotes() == handlerMethod.method.name
      apiOperation.getNickname() == handlerMethod.method.name
      apiOperation.getPosition() == 0
      apiOperation.getAuthorizations().size() == 0

      def secondApiOperation = operations[1]
      secondApiOperation.position == 1
  }


  def "Should ignore operations that are marked as hidden"() {

    given:
      RequestMappingInfo requestMappingInfo = requestMappingInfo("/doesNotMatterForThisTest",
              [
                      patternsRequestCondition      : patternsRequestCondition('/doesNotMatterForThisTest', '/somePath/{businessId:\\d+}'),
                      requestMethodsRequestCondition: requestMethodsRequestCondition(PATCH, POST)
              ]
      )

      HandlerMethod handlerMethod = dummyHandlerMethod("methodThatIsHidden")
      RequestMappingContext context = new RequestMappingContext(context(), requestMappingInfo, handlerMethod)

    when:
      def mock = Mock(DocumentationPluginsManager)
      mock.operation(_) >> new OperationBuilder().hidden(true).build()
      def operations = new ApiOperationReader(mock).read(context)

    then:
      0 == operations.size()
  }
}