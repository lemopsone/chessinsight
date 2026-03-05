Feature: 2FA

  @auth
  Scenario: Email second factor is required before JWT is issued
    Given a technical user is prepared for authentication checks
    When the user signs in with valid password as first factor
    Then the response requires an email one-time code
    When the user confirms sign in with the email one-time code
    Then JWT tokens are issued for the user

  @auth
  Scenario: Account is blocked after limited wrong password attempts
    Given a technical user is prepared for authentication checks
    When the user enters a wrong password 3 times
    Then the account is blocked
    And sign in with the correct password is rejected because account is blocked

  @auth
  Scenario: Recovery unlocks a blocked account and resets password
    Given a technical user is prepared for authentication checks
    And the account is blocked because of wrong password attempts
    When recovery is requested for the locked account
    And recovery is confirmed with a new password
    Then sign in with the old password fails
    And sign in with the rotated password requires second factor
    And confirming the second factor issues JWT tokens

  @auth
  Scenario: Planned password rotation keeps account usable
    Given a technical user is prepared for authentication checks
    And the user is fully authenticated with email second factor
    When the user rotates the password in the profile
    Then sign in with the old password fails
    And sign in with the rotated password requires second factor
    And confirming the second factor issues JWT tokens
