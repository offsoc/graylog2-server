/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { useState } from 'react';

import { getValueFromInput } from 'util/FormsUtils';
import { Input } from 'components/bootstrap';
import NumberUtils from 'util/NumberUtils';

type SizeBasedRotationStrategyConfigurationProps = {
  config: any;
  updateConfig: (...args: any[]) => void;
};

const SizeBasedRotationStrategyConfiguration = ({
  config,
  updateConfig,
}: SizeBasedRotationStrategyConfigurationProps) => {
  const { max_size } = config;
  const [maxSize, setMaxSize] = useState(max_size);

  const _onInputUpdate = (field) => (e) => {
    const update = {};
    const value = getValueFromInput(e.target);
    update[field] = value;

    setMaxSize(value);
    updateConfig(update);
  };

  const _formatSize = () => NumberUtils.formatBytes(maxSize);

  return (
    <div>
      <Input
        type="number"
        id="max-size"
        label="Max size per index (in bytes)"
        onChange={_onInputUpdate('max_size')}
        value={maxSize}
        help="Maximum size of an index before it gets rotated"
        addonAfter={_formatSize()}
        required
      />
    </div>
  );
};

export default SizeBasedRotationStrategyConfiguration;
