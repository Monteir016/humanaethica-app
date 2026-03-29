describe('Shift', () => {
  const LOCATION =
    'Cypress E2E shift location string for create success test';
  const PARTICIPANTS = '5';

  beforeEach(() => {
    cy.deleteAllButArs();
    cy.createDemoEntities();
    cy.createDatabaseInfoForShiftCreation();
  });

  afterEach(() => {
    cy.deleteAllButArs();
  });

  it('member creates a shift successfully', () => {
    cy.intercept('GET', '**/users/*/getInstitution').as('getInstitution');
    cy.intercept('POST', '**/activities/4/shift').as('createShift');

    cy.demoMemberLogin();

    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitution');

    cy.contains('[data-cy="memberActivitiesTable"] tbody tr', 'Shift E2E Activity')
      .find('[data-cy="manageShifts"]')
      .click();

    cy.get('[data-cy="activityShiftsTable"]', { timeout: 15000 }).should(
      'be.visible',
    );
    cy.get('[data-cy="activityShiftsTable"]').should(
      'contain',
      'No shifts available for this activity.',
    );

    cy.get('[data-cy="newShift"]').click();
    cy.get('[data-cy="locationInput"]').should('be.visible').clear().type(LOCATION);
    cy.get('[data-cy="participantsLimitInput"]').clear().type(PARTICIPANTS);

    cy.pickCtkDateTimeDay('startTimeInput', 0);
    cy.pickCtkDateTimeDay('endTimeInput', 1);

    cy.get('[data-cy="saveShift"]').should('not.be.disabled').click();

    cy.wait('@createShift').its('response.statusCode').should('eq', 200);

    cy.contains('[data-cy="activityShiftsTable"] tbody tr', LOCATION, {
      timeout: 20000,
    }).should('be.visible');
    cy.contains('[data-cy="activityShiftsTable"] tbody tr', LOCATION).within(
      () => {
        cy.get('td').eq(3).should('contain', PARTICIPANTS);
      },
    );
  });

  it('shows error and blocks save when shift start is after end', () => {
    cy.intercept('GET', '**/users/*/getInstitution').as('getInstitution');

    cy.demoMemberLogin();

    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitution');

    cy.contains('[data-cy="memberActivitiesTable"] tbody tr', 'Shift E2E Activity')
      .find('[data-cy="manageShifts"]')
      .click();

    cy.get('[data-cy="activityShiftsTable"]', { timeout: 15000 }).should(
      'be.visible',
    );

    cy.get('[data-cy="newShift"]').click();
    cy.get('[data-cy="locationInput"]').should('be.visible').clear().type(LOCATION);
    cy.get('[data-cy="participantsLimitInput"]').clear().type(PARTICIPANTS);

    // Later calendar day first, earlier second → start > end
    cy.pickCtkDateTimeDay('startTimeInput', 1);
    cy.pickCtkDateTimeDay('endTimeInput', 0);

    cy.get('[data-cy="shiftDateRangeError"]').should('be.visible');
    cy.get('[data-cy="saveShift"]').should('be.disabled');
  });

  it('shows error and blocks save when shift dates are outside activity period', () => {
    cy.deleteAllButArs();
    cy.createDemoEntities();
    cy.createDatabaseInfoForShiftCreationOutsideActivityPeriod();

    cy.intercept('GET', '**/users/*/getInstitution').as('getInstitution');

    cy.demoMemberLogin();

    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitution');

    cy.contains('[data-cy="memberActivitiesTable"] tbody tr', 'Shift E2E Activity')
      .find('[data-cy="manageShifts"]')
      .click();

    cy.get('[data-cy="activityShiftsTable"]', { timeout: 15000 }).should(
      'be.visible',
    );

    cy.get('[data-cy="newShift"]').click();
    cy.get('[data-cy="locationInput"]').should('be.visible').clear().type(LOCATION);
    cy.get('[data-cy="participantsLimitInput"]').clear().type(PARTICIPANTS);

    cy.pickCtkDateTimeDay('startTimeInput', 0);
    cy.pickCtkDateTimeDay('endTimeInput', 1);

    cy.get('[data-cy="shiftOutsideActivityPeriodError"]').should('be.visible');
    cy.get('[data-cy="saveShift"]').should('be.disabled');
  });

  it('disables New Shift when activity is not approved', () => {
    cy.deleteAllButArs();
    cy.createDemoEntities();
    cy.createDatabaseInfoForShiftCreationNotApproved();

    cy.intercept('GET', '**/users/*/getInstitution').as('getInstitution');

    cy.demoMemberLogin();

    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitution');

    cy.contains(
      '[data-cy="memberActivitiesTable"] tbody tr',
      'Shift E2E Not Approved Activity',
    )
      .find('[data-cy="manageShifts"]')
      .click();

    cy.get('[data-cy="activityShiftsTable"]', { timeout: 15000 }).should(
      'be.visible',
    );
    cy.get('[data-cy="newShift"]').should('be.disabled');
    cy.get('[data-cy="locationInput"]').should('not.exist');
  });
});
