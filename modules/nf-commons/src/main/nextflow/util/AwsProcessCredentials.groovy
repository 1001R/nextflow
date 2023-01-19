package nextflow.util

import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor

@EqualsAndHashCode
@TupleConstructor
class AwsProcessCredentials {
    String command
}
