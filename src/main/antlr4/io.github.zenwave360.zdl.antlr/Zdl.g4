grammar Zdl;

// Keywords
ENTITY: 'entity';
ENUM: 'enum';
INPUT: 'input';
EVENT: 'event';
RELATIONSHIP: 'relationship';
MANY_TO_MANY: 'ManyToMany';
MANY_TO_ONE: 'ManyToOne';
ONE_TO_MANY: 'OneToMany';
ONE_TO_ONE: 'OneToOne';
//fragment SERVICE_TOKEN: 'service';
//SERVICE: ~'@' SERVICE_TOKEN; // not starting with @
SERVICE: 'service';
WITH: 'with';
FOR: 'for';
WITH_EVENTS: 'withEvents';

// options with reserved tokens
ENTITY_OPTION: '@entity';
SERVICE_OPTION: '@service';
INPUT_OPTION: '@input';
EVENT_OPTION: '@event';
RELATIONSHIP_OPTION: '@relationship';
ENUM_OPTION: '@enum';
PAGEABLE_OPTION: '@pageable';

REQUIRED: 'required';
UNIQUE: 'unique';
MIN: 'min';
MAX: 'max';
MINLENGTH: 'minlength';
MAXLENGTH: 'maxlength';
PATTERN: 'pattern';
AT: '@';
ARRAY: '[]';

FIELD_SEPARATOR: ',';

fragment DIGIT : [0-9] ;

ID: [a-zA-Z_][a-zA-Z0-9_]*;
INT: DIGIT+ ;
NUMBER: DIGIT+ ([.,] DIGIT+)? ;
DOUBLE_QUOTED_STRING :  '"' (ESC | ~["\\])* '"' ;
SINGLE_QUOTED_STRING :  '\'' (ESC | ~['\\])* '\'' ;
fragment ESC :   '\\' ["\\/bfnrt] ;

LEGACY_CONSTANT: [A-Z0-9_]+ '=' DIGIT+;

VALUE : SINGLE_QUOTED_STRING | DOUBLE_QUOTED_STRING | NUMBER | 'true' | 'false' | 'null';
OBJECT: '{' (ID ':' VALUE)? (',' ID ':' VALUE)* '}';


// Comments
//SUFFIX_JAVADOC: {getCharPositionInLine() > 10}? '/**' .*? '*/';
//SUFFIX_JAVADOC: '/***' .*? '*/';
JAVADOC: '/**' .*? '*/';
LINE_COMMENT : '//' .*? '\r'? '\n' -> skip ; // Match "//" stuff '\n'
COMMENT : '/*' .*? '*/' -> skip ; // Match "/*" stuff "*/"

// Whitespace
WS: [ \t\r\n]+ -> skip;

PATTERN_REGEX: '/' .*? '/' ; // TODO: improve regex

// Rules
zdl: global_javadoc? legacy_constants? (entity | enum | input | event | relationships | service | service_legacy)* EOF;
global_javadoc: JAVADOC;
javadoc: JAVADOC;
suffix_javadoc: JAVADOC;

legacy_constants: LEGACY_CONSTANT*;

// @options
option: reserved_option ('(' option_value ')')? | '@' option_name ('(' option_value ')')?;
reserved_option: ENTITY_OPTION | SERVICE_OPTION | INPUT_OPTION | EVENT_OPTION | RELATIONSHIP_OPTION | ENUM_OPTION | PAGEABLE_OPTION;
option_name: ID;
option_value: ID | VALUE | OBJECT;

// entities
entity: javadoc? (option)* ENTITY entity_name entity_table_name? '{' fields '}';
entity_name: ID;
entity_table_name: '(' ID ')';

fields: (field FIELD_SEPARATOR?)*;
field: javadoc? (option)* field_name field_type entity_table_name? (field_validations)* suffix_javadoc? (nested_field)?;
nested_field: '{' (field)* '}';
field_name: ID;
field_type: ID | ID ARRAY;
//field_validations: REQUIRED | UNIQUE | min_validation | max_validation | minlength_validation | maxlength_validation | pattern_validation;
field_validations: field_validation_name ('(' field_validation_value ')')?;
field_validation_name: REQUIRED | UNIQUE | MIN | MAX | MINLENGTH | MAXLENGTH | PATTERN;
field_validation_value: INT | ID | PATTERN_REGEX;


// enums
enum: javadoc? (option)* ENUM enum_name '{' (enum_value FIELD_SEPARATOR?)* '}';
enum_name: ID;
enum_value: javadoc? enum_value_name ('(' enum_value_value ')')? suffix_javadoc?;
enum_value_name: ID;
enum_value_value: INT | ID | VALUE;

// inputs
input: javadoc? (option)* INPUT input_name '{' fields '}';
input_name: ID;

// events
event: javadoc? (option)* EVENT event_name ('(' event_channel ')')? '{' fields '}';
event_name: ID;
event_channel: ID;

// relationships
relationships: RELATIONSHIP relationship_type  '{' relationship* '}';
relationship_type: MANY_TO_MANY | MANY_TO_ONE| ONE_TO_MANY | ONE_TO_ONE;
relationship: relationship_from 'to'relationship_to;
relationship_from: relationship_javadoc? relationship_options relationship_definition;
relationship_to: relationship_javadoc? relationship_options relationship_definition;
relationship_javadoc: JAVADOC?;
relationship_options: (option)*;
relationship_definition: relationship_entity_name ('{' relationship_field_name relationship_description_field? '}')?;
relationship_entity_name: ID;
relationship_field_name: ID;
relationship_description_field: '(' ID ')';


// services
service: javadoc? (option)*  SERVICE ID FOR '(' service_aggregates ')' '{' service_method* '}';
service_aggregates: ID (',' ID)*;
service_method: javadoc? (option)* service_method_name '(' service_method_parameter_id? ','? service_method_parameter? ')' service_method_return? service_method_events?;
service_method_name: ID;
service_method_parameter_id: 'id';
service_method_parameter: ID;
service_method_return: ID | ID ARRAY;
service_method_events: WITH_EVENTS (service_method_event)*;
service_method_event: ID;

service_legacy: SERVICE service_aggregates 'with' ID;


