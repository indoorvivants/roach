//> using lib "org.scala-lang.modules::scala-xml:2.1.0"
//> using lib "com.indoorvivants::rendition:0.0.3"
//> using lib "com.lihaoyi::os-lib:0.9.3"

import scala.xml.*

import rendition.*

val text =
  """
<table class="table" summary="PostgreSQL Error Codes" border="1">
        <tbody>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 00 — Successful Completion</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">00000</code></td>
            <td><code class="symbol">successful_completion</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 01 — Warning</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">01000</code></td>
            <td><code class="symbol">warning</code></td>
          </tr>
          <tr>
            <td><code class="literal">0100C</code></td>
            <td><code class="symbol">dynamic_result_sets_returned</code></td>
          </tr>
          <tr>
            <td><code class="literal">01008</code></td>
            <td><code class="symbol">implicit_zero_bit_padding</code></td>
          </tr>
          <tr>
            <td><code class="literal">01003</code></td>
            <td><code class="symbol">null_value_eliminated_in_set_function</code></td>
          </tr>
          <tr>
            <td><code class="literal">01007</code></td>
            <td><code class="symbol">privilege_not_granted</code></td>
          </tr>
          <tr>
            <td><code class="literal">01006</code></td>
            <td><code class="symbol">privilege_not_revoked</code></td>
          </tr>
          <tr>
            <td><code class="literal">01004</code></td>
            <td><code class="symbol">string_data_right_truncation</code></td>
          </tr>
          <tr>
            <td><code class="literal">01P01</code></td>
            <td><code class="symbol">deprecated_feature</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 02 — No Data (this is also a warning class per the SQL standard)</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">02000</code></td>
            <td><code class="symbol">no_data</code></td>
          </tr>
          <tr>
            <td><code class="literal">02001</code></td>
            <td><code class="symbol">no_additional_dynamic_result_sets_returned</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 03 — SQL Statement Not Yet Complete</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">03000</code></td>
            <td><code class="symbol">sql_statement_not_yet_complete</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 08 — Connection Exception</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">08000</code></td>
            <td><code class="symbol">connection_exception</code></td>
          </tr>
          <tr>
            <td><code class="literal">08003</code></td>
            <td><code class="symbol">connection_does_not_exist</code></td>
          </tr>
          <tr>
            <td><code class="literal">08006</code></td>
            <td><code class="symbol">connection_failure</code></td>
          </tr>
          <tr>
            <td><code class="literal">08001</code></td>
            <td><code class="symbol">sqlclient_unable_to_establish_sqlconnection</code></td>
          </tr>
          <tr>
            <td><code class="literal">08004</code></td>
            <td><code class="symbol">sqlserver_rejected_establishment_of_sqlconnection</code></td>
          </tr>
          <tr>
            <td><code class="literal">08007</code></td>
            <td><code class="symbol">transaction_resolution_unknown</code></td>
          </tr>
          <tr>
            <td><code class="literal">08P01</code></td>
            <td><code class="symbol">protocol_violation</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 09 — Triggered Action Exception</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">09000</code></td>
            <td><code class="symbol">triggered_action_exception</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 0A — Feature Not Supported</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">0A000</code></td>
            <td><code class="symbol">feature_not_supported</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 0B — Invalid Transaction Initiation</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">0B000</code></td>
            <td><code class="symbol">invalid_transaction_initiation</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 0F — Locator Exception</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">0F000</code></td>
            <td><code class="symbol">locator_exception</code></td>
          </tr>
          <tr>
            <td><code class="literal">0F001</code></td>
            <td><code class="symbol">invalid_locator_specification</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 0L — Invalid Grantor</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">0L000</code></td>
            <td><code class="symbol">invalid_grantor</code></td>
          </tr>
          <tr>
            <td><code class="literal">0LP01</code></td>
            <td><code class="symbol">invalid_grant_operation</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 0P — Invalid Role Specification</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">0P000</code></td>
            <td><code class="symbol">invalid_role_specification</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 0Z — Diagnostics Exception</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">0Z000</code></td>
            <td><code class="symbol">diagnostics_exception</code></td>
          </tr>
          <tr>
            <td><code class="literal">0Z002</code></td>
            <td><code class="symbol">stacked_diagnostics_accessed_without_active_handler</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 20 — Case Not Found</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">20000</code></td>
            <td><code class="symbol">case_not_found</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 21 — Cardinality Violation</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">21000</code></td>
            <td><code class="symbol">cardinality_violation</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 22 — Data Exception</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">22000</code></td>
            <td><code class="symbol">data_exception</code></td>
          </tr>
          <tr>
            <td><code class="literal">2202E</code></td>
            <td><code class="symbol">array_subscript_error</code></td>
          </tr>
          <tr>
            <td><code class="literal">22021</code></td>
            <td><code class="symbol">character_not_in_repertoire</code></td>
          </tr>
          <tr>
            <td><code class="literal">22008</code></td>
            <td><code class="symbol">datetime_field_overflow</code></td>
          </tr>
          <tr>
            <td><code class="literal">22012</code></td>
            <td><code class="symbol">division_by_zero</code></td>
          </tr>
          <tr>
            <td><code class="literal">22005</code></td>
            <td><code class="symbol">error_in_assignment</code></td>
          </tr>
          <tr>
            <td><code class="literal">2200B</code></td>
            <td><code class="symbol">escape_character_conflict</code></td>
          </tr>
          <tr>
            <td><code class="literal">22022</code></td>
            <td><code class="symbol">indicator_overflow</code></td>
          </tr>
          <tr>
            <td><code class="literal">22015</code></td>
            <td><code class="symbol">interval_field_overflow</code></td>
          </tr>
          <tr>
            <td><code class="literal">2201E</code></td>
            <td><code class="symbol">invalid_argument_for_logarithm</code></td>
          </tr>
          <tr>
            <td><code class="literal">22014</code></td>
            <td><code class="symbol">invalid_argument_for_ntile_function</code></td>
          </tr>
          <tr>
            <td><code class="literal">22016</code></td>
            <td><code class="symbol">invalid_argument_for_nth_value_function</code></td>
          </tr>
          <tr>
            <td><code class="literal">2201F</code></td>
            <td><code class="symbol">invalid_argument_for_power_function</code></td>
          </tr>
          <tr>
            <td><code class="literal">2201G</code></td>
            <td><code class="symbol">invalid_argument_for_width_bucket_function</code></td>
          </tr>
          <tr>
            <td><code class="literal">22018</code></td>
            <td><code class="symbol">invalid_character_value_for_cast</code></td>
          </tr>
          <tr>
            <td><code class="literal">22007</code></td>
            <td><code class="symbol">invalid_datetime_format</code></td>
          </tr>
          <tr>
            <td><code class="literal">22019</code></td>
            <td><code class="symbol">invalid_escape_character</code></td>
          </tr>
          <tr>
            <td><code class="literal">2200D</code></td>
            <td><code class="symbol">invalid_escape_octet</code></td>
          </tr>
          <tr>
            <td><code class="literal">22025</code></td>
            <td><code class="symbol">invalid_escape_sequence</code></td>
          </tr>
          <tr>
            <td><code class="literal">22P06</code></td>
            <td><code class="symbol">nonstandard_use_of_escape_character</code></td>
          </tr>
          <tr>
            <td><code class="literal">22010</code></td>
            <td><code class="symbol">invalid_indicator_parameter_value</code></td>
          </tr>
          <tr>
            <td><code class="literal">22023</code></td>
            <td><code class="symbol">invalid_parameter_value</code></td>
          </tr>
          <tr>
            <td><code class="literal">22013</code></td>
            <td><code class="symbol">invalid_preceding_or_following_size</code></td>
          </tr>
          <tr>
            <td><code class="literal">2201B</code></td>
            <td><code class="symbol">invalid_regular_expression</code></td>
          </tr>
          <tr>
            <td><code class="literal">2201W</code></td>
            <td><code class="symbol">invalid_row_count_in_limit_clause</code></td>
          </tr>
          <tr>
            <td><code class="literal">2201X</code></td>
            <td><code class="symbol">invalid_row_count_in_result_offset_clause</code></td>
          </tr>
          <tr>
            <td><code class="literal">2202H</code></td>
            <td><code class="symbol">invalid_tablesample_argument</code></td>
          </tr>
          <tr>
            <td><code class="literal">2202G</code></td>
            <td><code class="symbol">invalid_tablesample_repeat</code></td>
          </tr>
          <tr>
            <td><code class="literal">22009</code></td>
            <td><code class="symbol">invalid_time_zone_displacement_value</code></td>
          </tr>
          <tr>
            <td><code class="literal">2200C</code></td>
            <td><code class="symbol">invalid_use_of_escape_character</code></td>
          </tr>
          <tr>
            <td><code class="literal">2200G</code></td>
            <td><code class="symbol">most_specific_type_mismatch</code></td>
          </tr>
          <tr>
            <td><code class="literal">22004</code></td>
            <td><code class="symbol">null_value_not_allowed</code></td>
          </tr>
          <tr>
            <td><code class="literal">22002</code></td>
            <td><code class="symbol">null_value_no_indicator_parameter</code></td>
          </tr>
          <tr>
            <td><code class="literal">22003</code></td>
            <td><code class="symbol">numeric_value_out_of_range</code></td>
          </tr>
          <tr>
            <td><code class="literal">2200H</code></td>
            <td><code class="symbol">sequence_generator_limit_exceeded</code></td>
          </tr>
          <tr>
            <td><code class="literal">22026</code></td>
            <td><code class="symbol">string_data_length_mismatch</code></td>
          </tr>
          <tr>
            <td><code class="literal">22001</code></td>
            <td><code class="symbol">string_data_right_truncation</code></td>
          </tr>
          <tr>
            <td><code class="literal">22011</code></td>
            <td><code class="symbol">substring_error</code></td>
          </tr>
          <tr>
            <td><code class="literal">22027</code></td>
            <td><code class="symbol">trim_error</code></td>
          </tr>
          <tr>
            <td><code class="literal">22024</code></td>
            <td><code class="symbol">unterminated_c_string</code></td>
          </tr>
          <tr>
            <td><code class="literal">2200F</code></td>
            <td><code class="symbol">zero_length_character_string</code></td>
          </tr>
          <tr>
            <td><code class="literal">22P01</code></td>
            <td><code class="symbol">floating_point_exception</code></td>
          </tr>
          <tr>
            <td><code class="literal">22P02</code></td>
            <td><code class="symbol">invalid_text_representation</code></td>
          </tr>
          <tr>
            <td><code class="literal">22P03</code></td>
            <td><code class="symbol">invalid_binary_representation</code></td>
          </tr>
          <tr>
            <td><code class="literal">22P04</code></td>
            <td><code class="symbol">bad_copy_file_format</code></td>
          </tr>
          <tr>
            <td><code class="literal">22P05</code></td>
            <td><code class="symbol">untranslatable_character</code></td>
          </tr>
          <tr>
            <td><code class="literal">2200L</code></td>
            <td><code class="symbol">not_an_xml_document</code></td>
          </tr>
          <tr>
            <td><code class="literal">2200M</code></td>
            <td><code class="symbol">invalid_xml_document</code></td>
          </tr>
          <tr>
            <td><code class="literal">2200N</code></td>
            <td><code class="symbol">invalid_xml_content</code></td>
          </tr>
          <tr>
            <td><code class="literal">2200S</code></td>
            <td><code class="symbol">invalid_xml_comment</code></td>
          </tr>
          <tr>
            <td><code class="literal">2200T</code></td>
            <td><code class="symbol">invalid_xml_processing_instruction</code></td>
          </tr>
          <tr>
            <td><code class="literal">22030</code></td>
            <td><code class="symbol">duplicate_json_object_key_value</code></td>
          </tr>
          <tr>
            <td><code class="literal">22031</code></td>
            <td><code class="symbol">invalid_argument_for_sql_json_datetime_function</code></td>
          </tr>
          <tr>
            <td><code class="literal">22032</code></td>
            <td><code class="symbol">invalid_json_text</code></td>
          </tr>
          <tr>
            <td><code class="literal">22033</code></td>
            <td><code class="symbol">invalid_sql_json_subscript</code></td>
          </tr>
          <tr>
            <td><code class="literal">22034</code></td>
            <td><code class="symbol">more_than_one_sql_json_item</code></td>
          </tr>
          <tr>
            <td><code class="literal">22035</code></td>
            <td><code class="symbol">no_sql_json_item</code></td>
          </tr>
          <tr>
            <td><code class="literal">22036</code></td>
            <td><code class="symbol">non_numeric_sql_json_item</code></td>
          </tr>
          <tr>
            <td><code class="literal">22037</code></td>
            <td><code class="symbol">non_unique_keys_in_a_json_object</code></td>
          </tr>
          <tr>
            <td><code class="literal">22038</code></td>
            <td><code class="symbol">singleton_sql_json_item_required</code></td>
          </tr>
          <tr>
            <td><code class="literal">22039</code></td>
            <td><code class="symbol">sql_json_array_not_found</code></td>
          </tr>
          <tr>
            <td><code class="literal">2203A</code></td>
            <td><code class="symbol">sql_json_member_not_found</code></td>
          </tr>
          <tr>
            <td><code class="literal">2203B</code></td>
            <td><code class="symbol">sql_json_number_not_found</code></td>
          </tr>
          <tr>
            <td><code class="literal">2203C</code></td>
            <td><code class="symbol">sql_json_object_not_found</code></td>
          </tr>
          <tr>
            <td><code class="literal">2203D</code></td>
            <td><code class="symbol">too_many_json_array_elements</code></td>
          </tr>
          <tr>
            <td><code class="literal">2203E</code></td>
            <td><code class="symbol">too_many_json_object_members</code></td>
          </tr>
          <tr>
            <td><code class="literal">2203F</code></td>
            <td><code class="symbol">sql_json_scalar_required</code></td>
          </tr>
          <tr>
            <td><code class="literal">2203G</code></td>
            <td><code class="symbol">sql_json_item_cannot_be_cast_to_target_type</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 23 — Integrity Constraint Violation</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">23000</code></td>
            <td><code class="symbol">integrity_constraint_violation</code></td>
          </tr>
          <tr>
            <td><code class="literal">23001</code></td>
            <td><code class="symbol">restrict_violation</code></td>
          </tr>
          <tr>
            <td><code class="literal">23502</code></td>
            <td><code class="symbol">not_null_violation</code></td>
          </tr>
          <tr>
            <td><code class="literal">23503</code></td>
            <td><code class="symbol">foreign_key_violation</code></td>
          </tr>
          <tr>
            <td><code class="literal">23505</code></td>
            <td><code class="symbol">unique_violation</code></td>
          </tr>
          <tr>
            <td><code class="literal">23514</code></td>
            <td><code class="symbol">check_violation</code></td>
          </tr>
          <tr>
            <td><code class="literal">23P01</code></td>
            <td><code class="symbol">exclusion_violation</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 24 — Invalid Cursor State</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">24000</code></td>
            <td><code class="symbol">invalid_cursor_state</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 25 — Invalid Transaction State</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">25000</code></td>
            <td><code class="symbol">invalid_transaction_state</code></td>
          </tr>
          <tr>
            <td><code class="literal">25001</code></td>
            <td><code class="symbol">active_sql_transaction</code></td>
          </tr>
          <tr>
            <td><code class="literal">25002</code></td>
            <td><code class="symbol">branch_transaction_already_active</code></td>
          </tr>
          <tr>
            <td><code class="literal">25008</code></td>
            <td><code class="symbol">held_cursor_requires_same_isolation_level</code></td>
          </tr>
          <tr>
            <td><code class="literal">25003</code></td>
            <td><code class="symbol">inappropriate_access_mode_for_branch_transaction</code></td>
          </tr>
          <tr>
            <td><code class="literal">25004</code></td>
            <td><code class="symbol">inappropriate_isolation_level_for_branch_transaction</code></td>
          </tr>
          <tr>
            <td><code class="literal">25005</code></td>
            <td><code class="symbol">no_active_sql_transaction_for_branch_transaction</code></td>
          </tr>
          <tr>
            <td><code class="literal">25006</code></td>
            <td><code class="symbol">read_only_sql_transaction</code></td>
          </tr>
          <tr>
            <td><code class="literal">25007</code></td>
            <td><code class="symbol">schema_and_data_statement_mixing_not_supported</code></td>
          </tr>
          <tr>
            <td><code class="literal">25P01</code></td>
            <td><code class="symbol">no_active_sql_transaction</code></td>
          </tr>
          <tr>
            <td><code class="literal">25P02</code></td>
            <td><code class="symbol">in_failed_sql_transaction</code></td>
          </tr>
          <tr>
            <td><code class="literal">25P03</code></td>
            <td><code class="symbol">idle_in_transaction_session_timeout</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 26 — Invalid SQL Statement Name</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">26000</code></td>
            <td><code class="symbol">invalid_sql_statement_name</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 27 — Triggered Data Change Violation</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">27000</code></td>
            <td><code class="symbol">triggered_data_change_violation</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 28 — Invalid Authorization Specification</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">28000</code></td>
            <td><code class="symbol">invalid_authorization_specification</code></td>
          </tr>
          <tr>
            <td><code class="literal">28P01</code></td>
            <td><code class="symbol">invalid_password</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 2B — Dependent Privilege Descriptors Still Exist</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">2B000</code></td>
            <td><code class="symbol">dependent_privilege_descriptors_still_exist</code></td>
          </tr>
          <tr>
            <td><code class="literal">2BP01</code></td>
            <td><code class="symbol">dependent_objects_still_exist</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 2D — Invalid Transaction Termination</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">2D000</code></td>
            <td><code class="symbol">invalid_transaction_termination</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 2F — SQL Routine Exception</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">2F000</code></td>
            <td><code class="symbol">sql_routine_exception</code></td>
          </tr>
          <tr>
            <td><code class="literal">2F005</code></td>
            <td><code class="symbol">function_executed_no_return_statement</code></td>
          </tr>
          <tr>
            <td><code class="literal">2F002</code></td>
            <td><code class="symbol">modifying_sql_data_not_permitted</code></td>
          </tr>
          <tr>
            <td><code class="literal">2F003</code></td>
            <td><code class="symbol">prohibited_sql_statement_attempted</code></td>
          </tr>
          <tr>
            <td><code class="literal">2F004</code></td>
            <td><code class="symbol">reading_sql_data_not_permitted</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 34 — Invalid Cursor Name</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">34000</code></td>
            <td><code class="symbol">invalid_cursor_name</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 38 — External Routine Exception</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">38000</code></td>
            <td><code class="symbol">external_routine_exception</code></td>
          </tr>
          <tr>
            <td><code class="literal">38001</code></td>
            <td><code class="symbol">containing_sql_not_permitted</code></td>
          </tr>
          <tr>
            <td><code class="literal">38002</code></td>
            <td><code class="symbol">modifying_sql_data_not_permitted</code></td>
          </tr>
          <tr>
            <td><code class="literal">38003</code></td>
            <td><code class="symbol">prohibited_sql_statement_attempted</code></td>
          </tr>
          <tr>
            <td><code class="literal">38004</code></td>
            <td><code class="symbol">reading_sql_data_not_permitted</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 39 — External Routine Invocation Exception</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">39000</code></td>
            <td><code class="symbol">external_routine_invocation_exception</code></td>
          </tr>
          <tr>
            <td><code class="literal">39001</code></td>
            <td><code class="symbol">invalid_sqlstate_returned</code></td>
          </tr>
          <tr>
            <td><code class="literal">39004</code></td>
            <td><code class="symbol">null_value_not_allowed</code></td>
          </tr>
          <tr>
            <td><code class="literal">39P01</code></td>
            <td><code class="symbol">trigger_protocol_violated</code></td>
          </tr>
          <tr>
            <td><code class="literal">39P02</code></td>
            <td><code class="symbol">srf_protocol_violated</code></td>
          </tr>
          <tr>
            <td><code class="literal">39P03</code></td>
            <td><code class="symbol">event_trigger_protocol_violated</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 3B — Savepoint Exception</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">3B000</code></td>
            <td><code class="symbol">savepoint_exception</code></td>
          </tr>
          <tr>
            <td><code class="literal">3B001</code></td>
            <td><code class="symbol">invalid_savepoint_specification</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 3D — Invalid Catalog Name</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">3D000</code></td>
            <td><code class="symbol">invalid_catalog_name</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 3F — Invalid Schema Name</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">3F000</code></td>
            <td><code class="symbol">invalid_schema_name</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 40 — Transaction Rollback</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">40000</code></td>
            <td><code class="symbol">transaction_rollback</code></td>
          </tr>
          <tr>
            <td><code class="literal">40002</code></td>
            <td><code class="symbol">transaction_integrity_constraint_violation</code></td>
          </tr>
          <tr>
            <td><code class="literal">40001</code></td>
            <td><code class="symbol">serialization_failure</code></td>
          </tr>
          <tr>
            <td><code class="literal">40003</code></td>
            <td><code class="symbol">statement_completion_unknown</code></td>
          </tr>
          <tr>
            <td><code class="literal">40P01</code></td>
            <td><code class="symbol">deadlock_detected</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 42 — Syntax Error or Access Rule Violation</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">42000</code></td>
            <td><code class="symbol">syntax_error_or_access_rule_violation</code></td>
          </tr>
          <tr>
            <td><code class="literal">42601</code></td>
            <td><code class="symbol">syntax_error</code></td>
          </tr>
          <tr>
            <td><code class="literal">42501</code></td>
            <td><code class="symbol">insufficient_privilege</code></td>
          </tr>
          <tr>
            <td><code class="literal">42846</code></td>
            <td><code class="symbol">cannot_coerce</code></td>
          </tr>
          <tr>
            <td><code class="literal">42803</code></td>
            <td><code class="symbol">grouping_error</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P20</code></td>
            <td><code class="symbol">windowing_error</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P19</code></td>
            <td><code class="symbol">invalid_recursion</code></td>
          </tr>
          <tr>
            <td><code class="literal">42830</code></td>
            <td><code class="symbol">invalid_foreign_key</code></td>
          </tr>
          <tr>
            <td><code class="literal">42602</code></td>
            <td><code class="symbol">invalid_name</code></td>
          </tr>
          <tr>
            <td><code class="literal">42622</code></td>
            <td><code class="symbol">name_too_long</code></td>
          </tr>
          <tr>
            <td><code class="literal">42939</code></td>
            <td><code class="symbol">reserved_name</code></td>
          </tr>
          <tr>
            <td><code class="literal">42804</code></td>
            <td><code class="symbol">datatype_mismatch</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P18</code></td>
            <td><code class="symbol">indeterminate_datatype</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P21</code></td>
            <td><code class="symbol">collation_mismatch</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P22</code></td>
            <td><code class="symbol">indeterminate_collation</code></td>
          </tr>
          <tr>
            <td><code class="literal">42809</code></td>
            <td><code class="symbol">wrong_object_type</code></td>
          </tr>
          <tr>
            <td><code class="literal">428C9</code></td>
            <td><code class="symbol">generated_always</code></td>
          </tr>
          <tr>
            <td><code class="literal">42703</code></td>
            <td><code class="symbol">undefined_column</code></td>
          </tr>
          <tr>
            <td><code class="literal">42883</code></td>
            <td><code class="symbol">undefined_function</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P01</code></td>
            <td><code class="symbol">undefined_table</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P02</code></td>
            <td><code class="symbol">undefined_parameter</code></td>
          </tr>
          <tr>
            <td><code class="literal">42704</code></td>
            <td><code class="symbol">undefined_object</code></td>
          </tr>
          <tr>
            <td><code class="literal">42701</code></td>
            <td><code class="symbol">duplicate_column</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P03</code></td>
            <td><code class="symbol">duplicate_cursor</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P04</code></td>
            <td><code class="symbol">duplicate_database</code></td>
          </tr>
          <tr>
            <td><code class="literal">42723</code></td>
            <td><code class="symbol">duplicate_function</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P05</code></td>
            <td><code class="symbol">duplicate_prepared_statement</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P06</code></td>
            <td><code class="symbol">duplicate_schema</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P07</code></td>
            <td><code class="symbol">duplicate_table</code></td>
          </tr>
          <tr>
            <td><code class="literal">42712</code></td>
            <td><code class="symbol">duplicate_alias</code></td>
          </tr>
          <tr>
            <td><code class="literal">42710</code></td>
            <td><code class="symbol">duplicate_object</code></td>
          </tr>
          <tr>
            <td><code class="literal">42702</code></td>
            <td><code class="symbol">ambiguous_column</code></td>
          </tr>
          <tr>
            <td><code class="literal">42725</code></td>
            <td><code class="symbol">ambiguous_function</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P08</code></td>
            <td><code class="symbol">ambiguous_parameter</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P09</code></td>
            <td><code class="symbol">ambiguous_alias</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P10</code></td>
            <td><code class="symbol">invalid_column_reference</code></td>
          </tr>
          <tr>
            <td><code class="literal">42611</code></td>
            <td><code class="symbol">invalid_column_definition</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P11</code></td>
            <td><code class="symbol">invalid_cursor_definition</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P12</code></td>
            <td><code class="symbol">invalid_database_definition</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P13</code></td>
            <td><code class="symbol">invalid_function_definition</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P14</code></td>
            <td><code class="symbol">invalid_prepared_statement_definition</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P15</code></td>
            <td><code class="symbol">invalid_schema_definition</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P16</code></td>
            <td><code class="symbol">invalid_table_definition</code></td>
          </tr>
          <tr>
            <td><code class="literal">42P17</code></td>
            <td><code class="symbol">invalid_object_definition</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 44 — WITH CHECK OPTION Violation</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">44000</code></td>
            <td><code class="symbol">with_check_option_violation</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 53 — Insufficient Resources</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">53000</code></td>
            <td><code class="symbol">insufficient_resources</code></td>
          </tr>
          <tr>
            <td><code class="literal">53100</code></td>
            <td><code class="symbol">disk_full</code></td>
          </tr>
          <tr>
            <td><code class="literal">53200</code></td>
            <td><code class="symbol">out_of_memory</code></td>
          </tr>
          <tr>
            <td><code class="literal">53300</code></td>
            <td><code class="symbol">too_many_connections</code></td>
          </tr>
          <tr>
            <td><code class="literal">53400</code></td>
            <td><code class="symbol">configuration_limit_exceeded</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 54 — Program Limit Exceeded</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">54000</code></td>
            <td><code class="symbol">program_limit_exceeded</code></td>
          </tr>
          <tr>
            <td><code class="literal">54001</code></td>
            <td><code class="symbol">statement_too_complex</code></td>
          </tr>
          <tr>
            <td><code class="literal">54011</code></td>
            <td><code class="symbol">too_many_columns</code></td>
          </tr>
          <tr>
            <td><code class="literal">54023</code></td>
            <td><code class="symbol">too_many_arguments</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 55 — Object Not In Prerequisite State</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">55000</code></td>
            <td><code class="symbol">object_not_in_prerequisite_state</code></td>
          </tr>
          <tr>
            <td><code class="literal">55006</code></td>
            <td><code class="symbol">object_in_use</code></td>
          </tr>
          <tr>
            <td><code class="literal">55P02</code></td>
            <td><code class="symbol">cant_change_runtime_param</code></td>
          </tr>
          <tr>
            <td><code class="literal">55P03</code></td>
            <td><code class="symbol">lock_not_available</code></td>
          </tr>
          <tr>
            <td><code class="literal">55P04</code></td>
            <td><code class="symbol">unsafe_new_enum_value_usage</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 57 — Operator Intervention</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">57000</code></td>
            <td><code class="symbol">operator_intervention</code></td>
          </tr>
          <tr>
            <td><code class="literal">57014</code></td>
            <td><code class="symbol">query_canceled</code></td>
          </tr>
          <tr>
            <td><code class="literal">57P01</code></td>
            <td><code class="symbol">admin_shutdown</code></td>
          </tr>
          <tr>
            <td><code class="literal">57P02</code></td>
            <td><code class="symbol">crash_shutdown</code></td>
          </tr>
          <tr>
            <td><code class="literal">57P03</code></td>
            <td><code class="symbol">cannot_connect_now</code></td>
          </tr>
          <tr>
            <td><code class="literal">57P04</code></td>
            <td><code class="symbol">database_dropped</code></td>
          </tr>
          <tr>
            <td><code class="literal">57P05</code></td>
            <td><code class="symbol">idle_session_timeout</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 58 — System Error (errors external to <span class="productname">PostgreSQL</span> itself)</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">58000</code></td>
            <td><code class="symbol">system_error</code></td>
          </tr>
          <tr>
            <td><code class="literal">58030</code></td>
            <td><code class="symbol">io_error</code></td>
          </tr>
          <tr>
            <td><code class="literal">58P01</code></td>
            <td><code class="symbol">undefined_file</code></td>
          </tr>
          <tr>
            <td><code class="literal">58P02</code></td>
            <td><code class="symbol">duplicate_file</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class 72 — Snapshot Failure</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">72000</code></td>
            <td><code class="symbol">snapshot_too_old</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class F0 — Configuration File Error</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">F0000</code></td>
            <td><code class="symbol">config_file_error</code></td>
          </tr>
          <tr>
            <td><code class="literal">F0001</code></td>
            <td><code class="symbol">lock_file_exists</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class HV — Foreign Data Wrapper Error (SQL/MED)</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">HV000</code></td>
            <td><code class="symbol">fdw_error</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV005</code></td>
            <td><code class="symbol">fdw_column_name_not_found</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV002</code></td>
            <td><code class="symbol">fdw_dynamic_parameter_value_needed</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV010</code></td>
            <td><code class="symbol">fdw_function_sequence_error</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV021</code></td>
            <td><code class="symbol">fdw_inconsistent_descriptor_information</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV024</code></td>
            <td><code class="symbol">fdw_invalid_attribute_value</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV007</code></td>
            <td><code class="symbol">fdw_invalid_column_name</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV008</code></td>
            <td><code class="symbol">fdw_invalid_column_number</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV004</code></td>
            <td><code class="symbol">fdw_invalid_data_type</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV006</code></td>
            <td><code class="symbol">fdw_invalid_data_type_descriptors</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV091</code></td>
            <td><code class="symbol">fdw_invalid_descriptor_field_identifier</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV00B</code></td>
            <td><code class="symbol">fdw_invalid_handle</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV00C</code></td>
            <td><code class="symbol">fdw_invalid_option_index</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV00D</code></td>
            <td><code class="symbol">fdw_invalid_option_name</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV090</code></td>
            <td><code class="symbol">fdw_invalid_string_length_or_buffer_length</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV00A</code></td>
            <td><code class="symbol">fdw_invalid_string_format</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV009</code></td>
            <td><code class="symbol">fdw_invalid_use_of_null_pointer</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV014</code></td>
            <td><code class="symbol">fdw_too_many_handles</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV001</code></td>
            <td><code class="symbol">fdw_out_of_memory</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV00P</code></td>
            <td><code class="symbol">fdw_no_schemas</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV00J</code></td>
            <td><code class="symbol">fdw_option_name_not_found</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV00K</code></td>
            <td><code class="symbol">fdw_reply_handle</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV00Q</code></td>
            <td><code class="symbol">fdw_schema_not_found</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV00R</code></td>
            <td><code class="symbol">fdw_table_not_found</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV00L</code></td>
            <td><code class="symbol">fdw_unable_to_create_execution</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV00M</code></td>
            <td><code class="symbol">fdw_unable_to_create_reply</code></td>
          </tr>
          <tr>
            <td><code class="literal">HV00N</code></td>
            <td><code class="symbol">fdw_unable_to_establish_connection</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class P0 — PL/pgSQL Error</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">P0000</code></td>
            <td><code class="symbol">plpgsql_error</code></td>
          </tr>
          <tr>
            <td><code class="literal">P0001</code></td>
            <td><code class="symbol">raise_exception</code></td>
          </tr>
          <tr>
            <td><code class="literal">P0002</code></td>
            <td><code class="symbol">no_data_found</code></td>
          </tr>
          <tr>
            <td><code class="literal">P0003</code></td>
            <td><code class="symbol">too_many_rows</code></td>
          </tr>
          <tr>
            <td><code class="literal">P0004</code></td>
            <td><code class="symbol">assert_failure</code></td>
          </tr>
          <tr>
            <td colspan="2"><span class="bold"><strong>Class XX — Internal Error</strong></span></td>
          </tr>
          <tr>
            <td><code class="literal">XX000</code></td>
            <td><code class="symbol">internal_error</code></td>
          </tr>
          <tr>
            <td><code class="literal">XX001</code></td>
            <td><code class="symbol">data_corrupted</code></td>
          </tr>
          <tr>
            <td><code class="literal">XX002</code></td>
            <td><code class="symbol">index_corrupted</code></td>
          </tr>
        </tbody>
      </table>
"""

@main def hello(file: String) =
  val path =
    try os.Path(file)
    catch
      case exc: java.lang.IllegalArgumentException =>
        os.RelPath(file).resolveFrom(os.pwd)
  val nodes = XML.loadString(text)
  val codes = Vector.newBuilder[String *: String *: EmptyTuple]
  (nodes \\ "tr").foreach { node =>
    val first = node \ "td"
    if first \@ "colspan" == "2" then
      val headerName = (first \\ "strong").head
    else
      val code = (node \\ "td")(0).text
      val title = (node \\ "td")(1).text

      codes addOne code -> title

  }

  val generatedCode = LineBuilder()

  val conflicts = codes.result().groupBy(_._2).filter(_._2.size > 1)

  println(conflicts.filter(_._2.size > 1))

  generatedCode.render { r =>
    import r.*
    r.use {
      // val narrowed = codes
      //   .result()
      //   .map(_._2)
      //   .map(s => '"'.toString() + s + '"'.toString)
      //   .grouped(5)
      //   .map(_.mkString("", " | ", " |"))
      //   .toVector
      // block("opaque type SQLSTATE = ", "") {
      //   narrowed.zipWithIndex.foreach{case (l, i) =>
      //     val nl = if i == narrowed.size - 1 then l.dropRight(" |".length) else l

      //     line(nl)
      //   }
      // }
      // block("type LookupResult[T <: SQLSTATE] = T match ", "") {
      //   conflicts.toVector.sortBy(_._1).foreach { case (title, values) =>
      //     val sortedValues = values.map(_._1).sorted
      //     line(
      //       s"case SQLSTATE.$title.type => ${sortedValues.mkString("\"", "\"  *: \"", "\"")} *: EmptyTuple"
      //     )
      //   }

      //   line("case _ => String")
      // }

      line("package roach")
      emptyLine()

      line("opaque type SQLSTATE = String")
      block("object SQLSTATE:", "end SQLSTATE") {
        line(
          "extension (inline v: SQLSTATE) transparent inline def code = inlineCode(v)"
        )
        codes.result().map(_._2).distinct.foreach { title =>
          line(s"inline def $title: SQLSTATE = \"$title\"")
        }
        emptyLine()
        block("def lookup(code: String): Option[SQLSTATE] = ", "end lookup") {
          block("code match", "") {
            codes.result().foreach { case (code, title) =>
              line(s"""case "$code" => Some($title) """.trim)

            }
            line("case _ => None")
          }
        }
        emptyLine()
        block(
          "inline def inlineLookup(inline code: String): SQLSTATE = ",
          "end inlineLookup"
        ) {
          block("inline code match", "") {
            codes.result().foreach { case (code, title) =>
              line(s"""case "$code" => $title """.trim)
            }
          }
        }
        emptyLine()
        line("@annotation.targetName(\"lookup_code\")")
        block(
          "def code(sqlState: SQLSTATE): Option[String] | List[String] = ",
          "end code"
        ) {
          block("sqlState match", "") {
            codes.result().foreach { case (code, title) =>
              conflicts.get(title) match
                case None =>
                  line(s"""case "$title" => Some("$code") """.trim)
                case Some(values) =>
                  line(s"""case "$title" => ${values
                      .map(_._1)
                      .mkString("List(\"", "\", \"", "\")")} """.trim)
            }
            line("case _ => None")
          }
        }
        emptyLine()
        block(
          "transparent inline def inlineCode(inline sqlState: SQLSTATE): String | (String, String) = ",
          "end inlineCode"
        ) {
          block("inline sqlState match", "") {
            codes.result().foreach { case (code, title) =>
              conflicts.get(title) match
                case None =>
                  line(s"""case "$title" => \"$code\" """.trim)
                case Some(values) =>
                  line(s"""case "$title" => ${values
                      .map(_._1)
                      .mkString("(\"", "\", \"", "\")")} """.trim)
            }
          }
        }
      }
    }
  }

  os.write.over(path, generatedCode.result)
end hello
