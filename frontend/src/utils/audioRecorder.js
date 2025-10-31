/**
 * 音频录制工具类
 * 使用 Web Audio API 录制音频并转换为 WAV 格式
 */
class AudioRecorder {
  constructor() {
    this.audioContext = null
    this.mediaStream = null
    this.scriptProcessor = null
    this.audioInput = null
    this.recording = false
    this.audioData = []
    this.sampleRate = 16000  // 16kHz 采样率，适合语音识别
  }

  /**
   * 开始录音
   */
  async start() {
    try {
      // 获取麦克风权限
      this.mediaStream = await navigator.mediaDevices.getUserMedia({ 
        audio: {
          echoCancellation: true,  // 回声消除
          noiseSuppression: true,  // 噪声抑制
          autoGainControl: true    // 自动增益控制
        } 
      })

      // 创建音频上下文
      this.audioContext = new (window.AudioContext || window.webkitAudioContext)()
      
      // 创建音频源
      this.audioInput = this.audioContext.createMediaStreamSource(this.mediaStream)
      
      // 创建处理节点（缓冲区大小 4096）
      const bufferSize = 4096
      this.scriptProcessor = this.audioContext.createScriptProcessor(bufferSize, 1, 1)
      
      // 清空音频数据
      this.audioData = []
      
      // 监听音频数据
      this.scriptProcessor.onaudioprocess = (event) => {
        if (!this.recording) return
        
        const inputData = event.inputBuffer.getChannelData(0)
        // 降采样到 16kHz
        const downsampled = this.downsample(inputData, this.audioContext.sampleRate, this.sampleRate)
        this.audioData.push(downsampled)
      }
      
      // 连接节点
      this.audioInput.connect(this.scriptProcessor)
      this.scriptProcessor.connect(this.audioContext.destination)
      
      this.recording = true
      return true
    } catch (error) {
      console.error('开始录音失败:', error)
      throw error
    }
  }

  /**
   * 停止录音并获取 WAV 格式的 Blob
   */
  async stop() {
    this.recording = false
    
    // 断开连接
    if (this.scriptProcessor) {
      this.scriptProcessor.disconnect()
      this.scriptProcessor = null
    }
    
    if (this.audioInput) {
      this.audioInput.disconnect()
      this.audioInput = null
    }
    
    // 停止媒体流
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(track => track.stop())
      this.mediaStream = null
    }
    
    // 关闭音频上下文
    if (this.audioContext) {
      await this.audioContext.close()
      this.audioContext = null
    }
    
    // 合并音频数据
    const audioBuffer = this.mergeAudioData(this.audioData)
    
    // 转换为 WAV 格式
    const wavBlob = this.encodeWAV(audioBuffer, this.sampleRate)
    
    return wavBlob
  }

  /**
   * 降采样
   */
  downsample(buffer, originalSampleRate, targetSampleRate) {
    if (originalSampleRate === targetSampleRate) {
      return buffer
    }
    
    const ratio = originalSampleRate / targetSampleRate
    const newLength = Math.round(buffer.length / ratio)
    const result = new Float32Array(newLength)
    
    let offsetResult = 0
    let offsetBuffer = 0
    
    while (offsetResult < result.length) {
      const nextOffsetBuffer = Math.round((offsetResult + 1) * ratio)
      let accum = 0
      let count = 0
      
      for (let i = offsetBuffer; i < nextOffsetBuffer && i < buffer.length; i++) {
        accum += buffer[i]
        count++
      }
      
      result[offsetResult] = accum / count
      offsetResult++
      offsetBuffer = nextOffsetBuffer
    }
    
    return result
  }

  /**
   * 合并音频数据
   */
  mergeAudioData(audioDataArray) {
    const totalLength = audioDataArray.reduce((acc, arr) => acc + arr.length, 0)
    const result = new Float32Array(totalLength)
    
    let offset = 0
    for (const arr of audioDataArray) {
      result.set(arr, offset)
      offset += arr.length
    }
    
    return result
  }

  /**
   * 将 Float32Array 转换为 Int16Array
   */
  floatTo16BitPCM(float32Array) {
    const int16Array = new Int16Array(float32Array.length)
    for (let i = 0; i < float32Array.length; i++) {
      const s = Math.max(-1, Math.min(1, float32Array[i]))
      int16Array[i] = s < 0 ? s * 0x8000 : s * 0x7FFF
    }
    return int16Array
  }

  /**
   * 写入字符串到 DataView
   */
  writeString(view, offset, string) {
    for (let i = 0; i < string.length; i++) {
      view.setUint8(offset + i, string.charCodeAt(i))
    }
  }

  /**
   * 将音频数据编码为 WAV 格式
   */
  encodeWAV(samples, sampleRate) {
    const buffer = new ArrayBuffer(44 + samples.length * 2)
    const view = new DataView(buffer)
    
    // RIFF 标识符
    this.writeString(view, 0, 'RIFF')
    // 文件长度
    view.setUint32(4, 36 + samples.length * 2, true)
    // WAVE 标识符
    this.writeString(view, 8, 'WAVE')
    // fmt 子块
    this.writeString(view, 12, 'fmt ')
    // fmt 子块大小
    view.setUint32(16, 16, true)
    // 音频格式（PCM = 1）
    view.setUint16(20, 1, true)
    // 声道数
    view.setUint16(22, 1, true)
    // 采样率
    view.setUint32(24, sampleRate, true)
    // 字节率 (采样率 * 声道数 * 每样本字节数)
    view.setUint32(28, sampleRate * 2, true)
    // 块对齐 (声道数 * 每样本字节数)
    view.setUint16(32, 2, true)
    // 位深度
    view.setUint16(34, 16, true)
    // data 子块
    this.writeString(view, 36, 'data')
    // data 子块大小
    view.setUint32(40, samples.length * 2, true)
    
    // 写入 PCM 数据
    const int16Samples = this.floatTo16BitPCM(samples)
    let offset = 44
    for (let i = 0; i < int16Samples.length; i++, offset += 2) {
      view.setInt16(offset, int16Samples[i], true)
    }
    
    return new Blob([view], { type: 'audio/wav' })
  }

  /**
   * 检查浏览器是否支持录音
   */
  static isSupported() {
    return !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia)
  }
}

export default AudioRecorder

