import { useState } from 'react';

import { useService } from '@flowgram.ai/free-layout-editor';
import { Button } from '@douyinfe/semi-ui';

import { RunningService } from '../../services';
import { useAgentConfigRun } from '../../contexts/agent-config-run-context';

/**
 * 在代理配置页：与代理列表「测试执行」一致，打开测试运行弹窗并流式执行。
 * 其他页面：画布仿真高亮连线。
 */
export function Run() {
  const [isRunning, setRunning] = useState(false);
  const { onRunClick } = useAgentConfigRun();
  const runningService = useService(RunningService);

  const onRun = async () => {
    if (onRunClick) {
      onRunClick();
      return;
    }
    setRunning(true);
    await runningService.startRun();
    setRunning(false);
  };

  return (
    <Button
      onClick={onRun}
      loading={!onRunClick && isRunning}
      style={{ backgroundColor: 'rgba(171,181,255,0.3)', borderRadius: '8px' }}
    >
      Run
    </Button>
  );
}
