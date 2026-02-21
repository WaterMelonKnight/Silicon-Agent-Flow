# Contributing to Silicon-Agent-Flow

First off, thank you for considering contributing to Silicon-Agent-Flow! It's people like you that make this project a great tool for the chip design community.

## Code of Conduct

This project and everyone participating in it is governed by our commitment to fostering an open and welcoming environment. We pledge to make participation in our project a harassment-free experience for everyone.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the existing issues to avoid duplicates. When you create a bug report, include as many details as possible:

- **Use a clear and descriptive title**
- **Describe the exact steps to reproduce the problem**
- **Provide specific examples** (code snippets, API requests, etc.)
- **Describe the behavior you observed** and what you expected
- **Include logs and error messages**
- **Specify your environment** (OS, Java version, Docker version)

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion:

- **Use a clear and descriptive title**
- **Provide a detailed description** of the suggested enhancement
- **Explain why this enhancement would be useful**
- **List any alternative solutions** you've considered

### Pull Requests

1. **Fork the repository** and create your branch from `main`
2. **Follow the coding style** (Google Java Style Guide)
3. **Write clear commit messages** following conventional commits
4. **Add tests** for new features
5. **Update documentation** as needed
6. **Ensure all tests pass** before submitting

#### Commit Message Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

Example:
```
feat(optimization): add multi-objective optimization support

Implement Pareto frontier analysis for balancing area, power, and timing.
Adds new OptimizationStrategy enum and updates OptimizationService.

Closes #123
```

## Development Setup

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose
- Git

### Local Development

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/silicon-agent-flow.git
cd silicon-agent-flow

# Add upstream remote
git remote add upstream https://github.com/ORIGINAL_OWNER/silicon-agent-flow.git

# Create a feature branch
git checkout -b feature/my-new-feature

# Install dependencies and build
mvn clean install

# Run tests
mvn test

# Run the application
mvn spring-boot:run
```

### Running Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Test coverage report
mvn jacoco:report
```

## Coding Standards

### Java Style Guide

Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html):

- **Indentation**: 4 spaces (no tabs)
- **Line length**: 100 characters
- **Naming conventions**:
  - Classes: `PascalCase`
  - Methods/variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- **Use Lombok** annotations to reduce boilerplate
- **Add JavaDoc** for public APIs

### Code Quality

- **No compiler warnings**: Fix all warnings before submitting
- **Test coverage**: Aim for >80% coverage for new code
- **Static analysis**: Code should pass SpotBugs and Checkstyle
- **Security**: Follow OWASP guidelines, no hardcoded secrets

### Example Code

```java
/**
 * Service for optimizing chip design parameters using LLM agents.
 *
 * @author Your Name
 * @since 1.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizationService {

    private final ChatClient chatClient;
    private final EdaJobRepository repository;

    /**
     * Analyzes job results and generates optimization suggestions.
     *
     * @param jobId the ID of the completed job
     * @return optimized parameters as a map
     * @throws IllegalArgumentException if job not found
     */
    public Map<String, Object> optimizeJob(Long jobId) {
        log.info("Starting optimization for job {}", jobId);
        // Implementation
    }
}
```

## Project Structure

```
src/main/java/com/silicon/agentflow/
├── config/          # Configuration classes
├── controller/      # REST API controllers
├── service/         # Business logic
├── repository/      # Data access layer
├── entity/          # JPA entities
├── dto/             # Data transfer objects
└── util/            # Utility classes

src/test/java/       # Test classes (mirror main structure)
```

## Testing Guidelines

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class OptimizationServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private EdaJobRepository repository;

    @InjectMocks
    private OptimizationService service;

    @Test
    void shouldOptimizeJobSuccessfully() {
        // Given
        EdaJob job = createTestJob();
        when(repository.findById(1L)).thenReturn(Optional.of(job));

        // When
        Map<String, Object> result = service.optimizeJob(1L);

        // Then
        assertThat(result).isNotNull();
        verify(chatClient).prompt();
    }
}
```

### Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class EdaJobControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldSubmitJobSuccessfully() throws Exception {
        mockMvc.perform(post("/api/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parameters\": {...}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }
}
```

## Documentation

### Code Documentation

- Add JavaDoc for all public classes and methods
- Include `@param`, `@return`, and `@throws` tags
- Provide usage examples for complex APIs

### README Updates

When adding new features, update:
- Feature list
- API documentation
- Configuration examples
- Quick start guide

## Review Process

1. **Automated checks**: CI/CD pipeline runs tests and linters
2. **Code review**: At least one maintainer reviews the PR
3. **Testing**: Reviewer tests the changes locally
4. **Approval**: PR is approved and merged

### Review Checklist

- [ ] Code follows style guidelines
- [ ] Tests are included and passing
- [ ] Documentation is updated
- [ ] No breaking changes (or properly documented)
- [ ] Commit messages follow conventions
- [ ] No merge conflicts

## Community

- **GitHub Discussions**: For questions and ideas
- **GitHub Issues**: For bugs and feature requests
- **Pull Requests**: For code contributions

## Recognition

Contributors will be recognized in:
- CHANGELOG.md for each release
- GitHub contributors page
- Special mentions for significant contributions

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing to Silicon-Agent-Flow! 🎉
