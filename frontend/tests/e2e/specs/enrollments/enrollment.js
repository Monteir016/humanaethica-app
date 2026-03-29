describe('Enrollment', () => {
  beforeEach(() => {
    cy.deleteAllButArs()
    cy.createDemoEntities();
    cy.createDatabaseInfoForEnrollments()
  });

  afterEach(() => {
    cy.deleteAllButArs()
  });

  it('create enrollment selecting two of three shifts', () => {
    const MOTIVATION = 'I am very keen to help other people.';

    cy.intercept('POST', '/enrollments').as('enroll');
    cy.intercept('GET', '/activities/1/enrollments').as('enrollments');

    cy.demoMemberLogin();
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .should('have.length', 3)
      .eq(0)
      .children()
      .should('have.length', 14)
      .eq(4)
      .should('contain', 0);
    cy.logout();

    cy.demoVolunteerLogin();
    cy.get('[data-cy="volunteerActivities"]').click();
    cy.contains('[data-cy="volunteerActivitiesTable"] tbody tr', 'A1')
      .find('[data-cy="applyButton"]')
      .click();
    cy.get('[data-cy="shiftIdsInput"]').click();
    cy.get('.v-menu__content').filter(':visible').find('.v-list-item').should('have.length', 3);
    cy.get('.v-menu__content')
      .filter(':visible')
      .contains('.v-list-item', '09:00')
      .click();
    cy.get('.v-menu__content')
      .filter(':visible')
      .contains('.v-list-item', '12:00')
      .click();
    cy.get('body').type('{esc}');
    cy.get('[data-cy="motivationInput"]').should('be.visible').type(MOTIVATION);
    cy.get('[data-cy="saveEnrollment"]').click();
    cy.wait('@enroll').then((interception) => {
      const raw = interception.request.body;
      const body = typeof raw === 'string' ? JSON.parse(raw) : raw;
      const shiftIds = body.shiftIds;
      expect(shiftIds).to.have.length(2);
      expect(shiftIds).to.have.members([1, 4]);
    });
    cy.logout();

    cy.demoMemberLogin();
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0)
      .children()
      .eq(4)
      .should('contain', 1);

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0)
      .find('[data-cy="showEnrollments"]')
      .click();
    cy.wait('@enrollments');
    // GET /activities/:id/enrollments returns one row per shift link; 2 shifts => 2 rows, same motivation.
    cy.get('[data-cy="activityEnrollmentsTable"] tbody tr')
      .should('have.length', 2)
      .each(($tr) => {
        cy.wrap($tr)
          .children()
          .should('have.length', 7)
          .eq(1)
          .should('contain', MOTIVATION);
      });

    cy.logout();
  });

  it('update an enrollment', () => {
    const MOTIVATION2 = 'Motivation Example Two';
    
    // volunteer login and edit an errollment
    cy.demoVolunteerLogin()
    cy.get('[data-cy="volunteerEnrollments"]').click()
    cy.get('[data-cy="volunteerEnrollmentsTable"] tbody tr')
    .eq(0)
    .find('[data-cy="updateEnrollmentButton"]').click()
    
    cy.get('[data-cy="motivationInput"]').clear().type(MOTIVATION2);
    cy.get('[data-cy="saveEnrollment"]').click()
    cy.logout();

    // member check if the motivation is the new one
    cy.demoMemberLogin()
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(1)
      .find('[data-cy="showEnrollments"]').click()

    cy.get('[data-cy="activityEnrollmentsTable"] tbody tr')
    .eq(0)
    .children()
    .eq(1)
    .should('contain', MOTIVATION2)
    
    cy.logout();

  });

  it('delete an enrollment', () => {
    const ACTIVTIY_NAME_ONE = 'A2';

    // volunteer delete an enrollment
    cy.demoVolunteerLogin()
    cy.get('[data-cy="volunteerEnrollments"]').click()
    cy.get('[data-cy="volunteerEnrollmentsTable"] tbody tr')
    .eq(0)
    .children()
    .eq(0).should('contain', ACTIVTIY_NAME_ONE);

    cy.get('[data-cy="volunteerEnrollmentsTable"] tbody tr')
    .eq(0)
    .find('[data-cy="deleteEnrollmentButton"]').click();
    
    // check where is the actvity that volunteer delete enrollment and check that we can enroll again
    cy.get('[data-cy="volunteerActivities"]').click();
    cy.get('[data-cy="volunteerActivitiesTable"] tbody tr')
    .eq(1)
    .children()
    .eq(0)
    .should('contain', ACTIVTIY_NAME_ONE);

    cy.get('[data-cy="volunteerActivitiesTable"] tbody tr')
    .eq(1)
    .children()
    .eq(10)
    .find('[data-cy="applyButton"]')
    .should('exist');

    cy.logout();

    // member verify that this Activity don't have enrollments
    cy.demoMemberLogin()
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(1)
      .find('[data-cy="showEnrollments"]').click()
    
    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .should('have.length', 0)

    cy.logout();
    
  });
});