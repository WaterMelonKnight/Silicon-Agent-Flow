#!/usr/bin/env python3
"""
EDA Worker - 模拟芯片设计任务执行
接收参数，休眠 5 秒，随机返回面积数值
"""

import time
import random
import json
import sys
from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route('/health', methods=['GET'])
def health():
    """健康检查接口"""
    return jsonify({"status": "healthy"}), 200

@app.route('/execute', methods=['POST'])
def execute_job():
    """
    执行 EDA 任务
    接收参数：
    {
        "job_id": 1,
        "parameters": {
            "design_name": "chip_v1",
            "technology": "28nm",
            "target_frequency": "1GHz"
        }
    }
    """
    try:
        data = request.get_json()
        job_id = data.get('job_id')
        parameters = data.get('parameters', {})

        print(f"[Worker] Received job {job_id} with parameters: {parameters}")
        print(f"[Worker] Starting EDA simulation...")

        # 模拟 EDA 工具执行（休眠 5 秒）
        time.sleep(5)

        # 随机生成结果指标
        area = round(random.uniform(1000.0, 5000.0), 2)  # 面积 (um²)
        power = round(random.uniform(50.0, 200.0), 2)    # 功耗 (mW)
        frequency = round(random.uniform(800.0, 1200.0), 2)  # 频率 (MHz)

        result_metrics = {
            "area_um2": area,
            "power_mw": power,
            "frequency_mhz": frequency,
            "timing_met": random.choice([True, False]),
            "design_name": parameters.get("design_name", "unknown")
        }

        print(f"[Worker] Job {job_id} completed successfully")
        print(f"[Worker] Results: {json.dumps(result_metrics, indent=2)}")

        return jsonify({
            "status": "success",
            "job_id": job_id,
            "result_metrics": result_metrics,
            "log": f"EDA simulation completed for {parameters.get('design_name', 'unknown')}"
        }), 200

    except Exception as e:
        print(f"[Worker] Error executing job: {str(e)}", file=sys.stderr)
        return jsonify({
            "status": "failed",
            "error": str(e)
        }), 500

if __name__ == '__main__':
    print("[Worker] EDA Worker starting on port 5000...")
    app.run(host='0.0.0.0', port=5000, debug=True)
