/*
 * Copyright 2020-2022, Seqera Labs
 * Copyright 2013-2019, Centre for Genomic Regulation (CRG)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nextflow.cloud.aws

import com.amazonaws.AmazonClientException
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.InstanceMetadataRegionProvider
import com.amazonaws.regions.Region
import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.batch.AWSBatchClient
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ecs.AmazonECS
import com.amazonaws.services.ecs.AmazonECSClientBuilder
import com.amazonaws.services.logs.AWSLogs
import com.amazonaws.services.logs.AWSLogsAsyncClientBuilder
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest
import groovy.transform.CompileStatic
import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import nextflow.Global
import nextflow.exception.AbortOperationException
import nextflow.util.AwsSessionCredentials
import nextflow.util.AwsUserCredentials

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class AmazonClientFactory {

    /**
     * Reference to {@link AmazonEC2Client} object
     */
    private AmazonEC2Client ec2Client

    /**
     * The AWS region eg. {@code eu-west-1}. If it's not specified the current region is retrieved from
     * the EC2 instance metadata
     */
    private String region

    private AWSCredentialsProvider credentialsProvider

    /**
     * Initialise the Amazon cloud driver with default (empty) parameters
     */
    AmazonClientFactory() {
        this(Collections.emptyMap())
    }

    /**
     * Initialise the Amazon cloud driver with the specified parameters
     *
     * @param config
     *      A map holding the driver parameters:
     *      - accessKey: the access key credentials
     *      - secretKey: the secret key credentials
     *      - region: the AWS region
     */
    AmazonClientFactory(Map config) {
        // -- get the aws credentials
        def credentials
        if( config.accessKey && config.secretKey ) {
            String accessKey = config.accessKey
            String secretKey = config.secretKey
            String token = config.sessionToken
            credentials = config.sessionToken
                    ? new AwsSessionCredentials(accessKey, secretKey, token)
                    : new AwsUserCredentials(accessKey, secretKey)
        }
        else {
            credentials = Global.getAwsCredentials()
        }

        if( !credentials && !fetchIamRole() )
            throw new AbortOperationException("Missing AWS security credentials -- Provide access/security keys pair or define an IAM instance profile (suggested)")

        // -- get the aws default region
        region = config.region ?: Global.getAwsRegion() ?: fetchRegion()
        if( !region )
            throw new AbortOperationException('Missing AWS region -- Make sure to define in your system environment the variable `AWS_DEFAULT_REGION`')

        credentialsProvider = CredentialsProviderFactory.INSTANCE.createCredentialsProvider(credentials)
    }

    /**
     * Retrieve the current IAM role eventually define for a EC2 instance.
     * See http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/iam-roles-for-amazon-ec2.html#instance-metadata-security-credentials
     *
     * @return
     *      The IAM role name associated to this instance or {@code null} if no role is defined or
     *      it's not a EC2 instance
     */
    private String fetchIamRole() {
        try {
            def stsClient = AWSSecurityTokenServiceClientBuilder.defaultClient();
            return stsClient.getCallerIdentity(new GetCallerIdentityRequest()).getArn()
        }
        catch( AmazonClientException e ) {
            log.trace "Unable to fetch IAM credentials -- Cause: ${e.message}"
            return null
        }
    }

    /**
     * Retrieve the AWS region from the EC2 instance metadata.
     * See http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html
     *
     * @return
     *      The AWS region of the current EC2 instance eg. {@code eu-west-1} or
     *      {@code null} if it's not an EC2 instance.
     */
    private String fetchRegion() {
        try {
            return new InstanceMetadataRegionProvider().getRegion()
        }
        catch (AmazonClientException e) {
            log.debug "Cannot fetch AWS region", e
            return null
        }
    }

    /**
     * Helper method to map a region string to a {@link Region} object.
     *
     * @param region An AWS region string identifier eg. {@code eu-west-1}
     * @return A {@link Region} corresponding to the specified region string
     */
    private Region getRegionObj(String region) {
        final result = RegionUtils.getRegion(region)
        if( !result )
            throw new IllegalArgumentException("Not a valid AWS region name: $region");
        return result
    }

    /**
     * Gets or lazily creates an {@link AmazonEC2Client} instance given the current
     * configuration parameter
     *
     * @return
     *      An {@link AmazonEC2Client} instance
     */
    synchronized AmazonEC2Client getEc2Client() {

        if( ec2Client )
            return ec2Client

        def result = new AmazonEC2Client(credentialsProvider)

        if( region )
            result.setRegion(getRegionObj(region))

        return result
    }



    /**
     * Gets or lazily creates an {@link AWSBatchClient} instance given the current
     * configuration parameter
     *
     * @return
     *      An {@link AWSBatchClient} instance
     */
    @Memoized
    AWSBatchClient getBatchClient() {
        AWSBatchClient result = new AWSBatchClient(credentialsProvider)
        if( region )
            result.setRegion(getRegionObj(region))

        return result
    }

    @Memoized
    AmazonECS getEcsClient() {

        final clientBuilder = AmazonECSClientBuilder.standard()
            .withCredentials(credentialsProvider)
        if( region )
            clientBuilder.withRegion(region)

        clientBuilder.build()
    }

    @Memoized
    AWSLogs getLogsClient() {

        final clientBuilder = AWSLogsAsyncClientBuilder.standard()
        if( region )
            clientBuilder.withRegion(region)

        clientBuilder.withCredentials(credentialsProvider)

        return clientBuilder.build()
    }

}
