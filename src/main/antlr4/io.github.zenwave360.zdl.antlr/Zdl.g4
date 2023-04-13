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
SERVICE_OPTION: '@service';
SERVICE: 'service';
WITH: 'with';
FOR: 'for';
WITH_EVENTS: 'withEvents';

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
zdl: global_javadoc? (option)* (entity | enum | input | event | relationships | service)* EOF;
global_javadoc: JAVADOC;
javadoc: JAVADOC;
suffix_javadoc: JAVADOC;

// @options
option: SERVICE_OPTION ('(' option_value ')')? | '@' option_name ('(' option_value ')')?;
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
field_validations: REQUIRED | UNIQUE | min_validation | max_validation | minlength_validation | maxlength_validation | pattern_validation;
validation_value_int: INT;
min_validation: MIN '(' validation_value_int ')';
max_validation: MAX '(' validation_value_int ')';
minlength_validation: MINLENGTH '(' validation_value_int ')';
maxlength_validation: MAXLENGTH '(' validation_value_int ')';
pattern_validation: PATTERN '(' PATTERN_REGEX ')';

// enums
enum: javadoc? (option)* ENUM enum_name '{' (enum_value FIELD_SEPARATOR?)* '}';
enum_name: ID;
enum_value: javadoc? enum_value_name ('(' enum_value_value ')')? suffix_javadoc?;
enum_value_name: ID;
enum_value_value: INT;// ID | VALUE | OBJECT;

// inputs
input: javadoc? (option)* INPUT input_name '{' fields '}';
input_name: ID;

// events
event: javadoc? (option)* EVENT event_name '{' fields '}';
event_name: ID;

// relationships
relationships: RELATIONSHIP (MANY_TO_MANY | MANY_TO_ONE| ONE_TO_MANY | ONE_TO_ONE)  '{' relationship* '}';
relationship:
    relationship_from_javadoc? relationship_from_options relationship_from_type ('{' relationship_from_field '}')?
    'to'
    relationship_to_javadoc? relationship_to_options relationship_to_type ('{' relationship_to_field '}')?;
relationship_from_javadoc: JAVADOC?;
relationship_from_options: (option)*;
relationship_to_javadoc: JAVADOC?;
relationship_to_options: (option)*;
relationship_from_type: ID;
relationship_from_field: ID;
relationship_to_type: ID;
relationship_to_field: ID;

// services
service: javadoc? (option)*  SERVICE ID sevice_aggregates '{' service_method* '}';
sevice_aggregates: FOR '(' ID (',' ID)* ')';
service_method: javadoc? (option)* service_method_name '(' service_method_parameter_id? ','? service_method_parameter? ')' service_method_return? service_method_events?;
service_method_name: ID;
service_method_parameter_id: 'id';
service_method_parameter: ID;
service_method_return: ID;
service_method_events: WITH_EVENTS (service_method_event)*;
service_method_event: ID;



