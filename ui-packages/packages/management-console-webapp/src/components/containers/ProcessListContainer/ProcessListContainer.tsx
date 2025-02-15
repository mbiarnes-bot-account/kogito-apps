/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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

import React, { useEffect } from 'react';
import { useHistory } from 'react-router-dom';
import { OUIAProps } from '@kogito-apps/ouia-tools';
import { EmbeddedProcessList } from '@kogito-apps/process-list';
import { ProcessListGatewayApi } from '../../../channel/ProcessList';
import { useProcessListGatewayApi } from '../../../channel/ProcessList/ProcessListContext';
import {
  ProcessInstance,
  ProcessListState
} from '@kogito-apps/management-console-shared';

interface ProcessListContainerProps {
  initialState: ProcessListState;
}

const ProcessListContainer: React.FC<ProcessListContainerProps & OUIAProps> = ({
  initialState,
  ouiaId,
  ouiaSafe
}) => {
  const history = useHistory();
  const gatewayApi: ProcessListGatewayApi = useProcessListGatewayApi();

  useEffect(() => {
    const unsubscriber = gatewayApi.onOpenProcessListen({
      onOpen(process: ProcessInstance) {
        history.push({
          pathname: `/Process/${process.id}`,
          state: gatewayApi.processListState
        });
      }
    });
    return () => {
      unsubscriber.unSubscribe();
    };
  }, []);

  return (
    <EmbeddedProcessList
      driver={gatewayApi}
      targetOrigin={window.location.origin}
      initialState={initialState}
      singularProcessLabel={'Process'}
      pluralProcessLabel={'Processes'}
    />
  );
};

export default ProcessListContainer;
