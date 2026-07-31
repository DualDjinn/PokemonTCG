# FE

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 20.3.22.

## Base folder structure (feature-driven)

```text
src/app/
  core/
    constants/
    guards/
    interceptors/
    services/
  shared/
    components/
    directives/
    models/
    pipes/
    ui/
  features/
    pokedex/
      components/
      data-access/services/
      domain/models/
      pages/pokedex-page/
  app.config.ts
  app.routes.ts
```

How it works:

- `core`: singleton services and cross-app technical concerns.
- `shared`: reusable UI and utilities with no business ownership.
- `features`: business domains; each feature owns routes, pages, components, models, and data access.
- `app.routes.ts`: only entry routes/lazy loading for features.

Rule of thumb: if code belongs to one domain, put it inside that feature; if it is generic and reusable, move it to `shared`; if it is global infrastructure, place it in `core`.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Karma](https://karma-runner.github.io) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
