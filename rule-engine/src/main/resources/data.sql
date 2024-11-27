-- Create the rule configuration table
CREATE TABLE IF NOT EXISTS c2o_re_bus_rule_config (
    rule_id BIGINT PRIMARY KEY,
    rule_code VARCHAR(255) NOT NULL UNIQUE,
    input_schema CLOB NOT NULL
);

-- Insert move account validation rule
INSERT INTO c2o_re_bus_rule_config (rule_id, rule_code, input_schema) 
VALUES (1, 'MOVE_ACCOUNT_VALIDATE', '{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Move account request input",
  "type": "object",
  "properties": {
    "sourceContract": {
      "type": "number"
    },
    "targetContract": {
      "type": "number"
    },
    "shipTos": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "obaNumbers": {
            "type": "array",
            "items": {
              "type": "string"
            }
          },
          "targetBillTo": {
            "type": "string"
          }
        },
        "required": [
          "obaNumbers",
          "targetBillTo"
        ]
      }
    },
    "billTos": {
      "type": "array",
      "items": {
        "type": "string"
      }
    }
  },
  "required": [
    "sourceContract",
    "targetContract"
  ]
}');
