# Contributing to RR-Bank

Thank you for your interest in contributing to RR-Bank. This document outlines the process and guidelines for contributing.

## Getting Started

1. Fork the repository
2. Clone your fork locally
3. Set up the development environment (see [docs/development.md](docs/development.md))
4. Create a feature branch from `main`

## Branch Naming

Use descriptive branch names with a type prefix:

```
feature/add-account-statements
fix/ledger-balance-cache-invalidation
refactor/transaction-saga-cleanup
docs/update-api-reference
```

## Commit Messages

Write clear, concise commit messages:

- Use the imperative mood ("Add feature" not "Added feature")
- First line: 50 characters or less summarizing the change
- Blank line, then a body explaining *why* the change was made if not obvious

```
Add daily transaction limit override for premium accounts

Premium accounts need configurable daily limits that differ from
the default $10,000 threshold. This adds a limit_override column
to the accounts table and updates the fraud check logic.
```

## Pull Request Process

1. Ensure your branch is up to date with `main`
2. Verify all services start successfully with `docker-compose up -d`
3. Test your changes manually through the relevant frontend or API
4. Open a pull request with:
   - A clear title describing the change
   - A description of what was changed and why
   - Steps to test the change
5. Address review feedback promptly

## Code Standards

### Backend (Java)

- Follow existing package structure: `controller`, `service`, `repository`, `entity`, `dto`
- Use constructor injection for dependencies
- Keep controllers thin -- business logic belongs in service classes
- Add appropriate error handling and meaningful error messages

### Frontend (TypeScript/React)

- Use functional components with hooks
- Follow existing Material-UI patterns for consistency
- Place API calls in the `src/api/` layer, not in components directly
- Use Redux Toolkit slices for shared state

### General

- Do not commit secrets, credentials, or environment-specific configuration
- Keep changes focused -- one logical change per PR
- Update relevant documentation when changing behavior

## Reporting Issues

When reporting a bug, include:

- Steps to reproduce
- Expected behavior
- Actual behavior
- Relevant service logs (`docker-compose logs <service>`)
- Environment details (OS, Docker version)

## Code of Conduct

Be respectful and constructive in all interactions. Focus on the technical merits of contributions and provide helpful feedback.
