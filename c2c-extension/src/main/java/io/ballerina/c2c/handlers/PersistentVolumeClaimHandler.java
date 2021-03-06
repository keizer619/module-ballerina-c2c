/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.c2c.handlers;

import io.ballerina.c2c.KubernetesConstants;
import io.ballerina.c2c.exceptions.KubernetesPluginException;
import io.ballerina.c2c.models.PersistentVolumeClaimModel;
import io.ballerina.c2c.utils.KubernetesUtils;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.QuantityBuilder;
import io.fabric8.kubernetes.client.internal.SerializationUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static io.ballerina.c2c.KubernetesConstants.VOLUME_CLAIM_FILE_POSTFIX;

/**
 * Generates kubernetes secret.
 */
public class PersistentVolumeClaimHandler extends AbstractArtifactHandler {

    private void generate(PersistentVolumeClaimModel volumeClaimModel) throws KubernetesPluginException {
        Quantity quantity = new QuantityBuilder()
                .withAmount(volumeClaimModel.getVolumeClaimSizeAmount())
                .withFormat(volumeClaimModel.getVolumeClaimSizeFormat())
                .build();

        Map<String, Quantity> requests = new HashMap<>();
        requests.put("storage", quantity);
        PersistentVolumeClaim claim = new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withName(volumeClaimModel.getName())
                .withNamespace(dataHolder.getNamespace())
                .withAnnotations(volumeClaimModel.getAnnotations())
                .endMetadata()
                .withNewSpec()
                .withAccessModes(volumeClaimModel.getAccessMode())
                .withNewResources()
                .withRequests(requests)
                .endResources()
                .endSpec()
                .build();
        try {
            String claimContent = SerializationUtils.dumpWithoutRuntimeStateAsYaml(claim);
            String outputFileName = VOLUME_CLAIM_FILE_POSTFIX + KubernetesConstants.YAML;
            if (dataHolder.isSingleYaml()) {
                outputFileName = claim.getMetadata().getName() + KubernetesConstants.YAML;
            }
            KubernetesUtils.writeToFile(claimContent, outputFileName);
        } catch (IOException e) {
            String errorMessage = "error while generating yaml file for volume claim: " + volumeClaimModel.getName();
            throw new KubernetesPluginException(errorMessage, e);
        }
    }


    @Override
    public void createArtifacts() throws KubernetesPluginException {
        int count = 0;
        Collection<PersistentVolumeClaimModel> volumeClaims = dataHolder.getVolumeClaimModelSet();
        if (volumeClaims.size() > 0) {
            OUT.println();
        }
        for (PersistentVolumeClaimModel claimModel : volumeClaims) {
            count++;
            generate(claimModel);
            OUT.print("\t@kubernetes:VolumeClaim \t\t - complete " + count + "/" + volumeClaims.size() + "\r");
        }
    }
}
