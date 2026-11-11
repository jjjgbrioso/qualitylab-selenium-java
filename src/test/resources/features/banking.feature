@banking
Feature: Banking
  A customer chooses Banking on the home page, signs in, and manages peso accounts.
  Balances change as people use the demo, so scenarios compare before and after.

  @smoke @login
  Scenario: Sign in to Banking with valid credentials
    Given I open the home page
    When I choose the "Banking" module
    And I sign in with the test customer's credentials
    Then I am signed in to the "Banking" module

  @balance @smoke
  Scenario: The total balance adds up
    Given I am signed in to the "Banking" module as the test customer
    Then the total balance equals the sum of my account balances

  @deposit
  Scenario: Deposit money
    Given I am signed in to the "Banking" module as the test customer
    When I deposit a unique amount using my checking account
    Then the transaction succeeds with a reference starting with "TXN-"
    And the account balance has gone up by that amount

  @withdrawal
  Scenario: Withdraw money
    Given I am signed in to the "Banking" module as the test customer
    When I withdraw a unique amount using my checking account
    Then the transaction succeeds with a reference starting with "TXN-"
    And the account balance has gone down by that amount

  @transfer @smoke
  Scenario: Transfer money to another customer
    Given I am signed in to the "Banking" module as the test customer
    When I transfer a unique amount from my checking account to account number "1000000003"
    Then the transaction succeeds with a reference starting with "TRF-"
    And the account balance has gone down by that amount

  @withdrawal @negative
  Scenario: A withdrawal larger than the balance is refused
    Given I am signed in to the "Banking" module as the test customer
    When I try to withdraw more than the balance of my checking account
    Then the transaction is refused with "The account balance is too low for this transaction."
    And the account balance is unchanged

  @transfer @negative
  Scenario Outline: Invalid transfer details are refused
    Given I am signed in to the "Banking" module as the test customer
    When I try to transfer "<amount>" from my checking account to account number "<to>"
    Then the transaction is refused with "<message>"
    And the account balance is unchanged

    Examples:
      | to         | amount | message                                    |
      | 12345      | 10.00  | Account number must be exactly 10 digits.  |
      | 9999999999 | 10.00  | No account matches this value.             |
      | 1000000001 | 10.00  | Choose a different destination account.    |
      | 1000000003 | 0      | Amount must be greater than zero.          |

  @history
  Scenario: Transaction history shows a new deposit
    Given I am signed in to the "Banking" module as the test customer
    When I deposit a unique amount using my checking account
    Then the transaction succeeds with a reference starting with "TXN-"
    And the account history lists that transaction as a DEPOSIT
