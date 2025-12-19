package com.zd.scnoteservice.exception;

/**
 * 笔记业务异常
 * 
 * <p>用于处理笔记相关的业务异常</p>
 * <p>包括笔记不存在、权限不足、参数错误等场景</p>
 * 
 * @author System
 * @since 2024-01-21
 */
public class NoteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final int code;

    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误信息
     */
    public NoteException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误信息
     * @param cause 原因异常
     */
    public NoteException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 获取错误码
     * 
     * @return 错误码
     */
    public int getCode() {
        return code;
    }

    /**
     * 笔记不存在异常
     * 
     * @param noteId 笔记ID
     * @return 异常实例
     */
    public static NoteException noteNotFound(String noteId) {
        return new NoteException(404, "笔记不存在：" + noteId);
    }

    /**
     * 权限不足异常
     * 
     * @param operation 操作类型
     * @return 异常实例
     */
    public static NoteException accessDenied(String operation) {
        return new NoteException(403, "无权进行此操作：" + operation);
    }

    /**
     * 参数错误异常
     * 
     * @param message 错误信息
     * @return 异常实例
     */
    public static NoteException invalidParameter(String message) {
        return new NoteException(400, "参数错误：" + message);
    }

    /**
     * 文档不存在异常
     * 
     * @param documentId 文档ID
     * @return 异常实例
     */
    public static NoteException documentNotFound(String documentId) {
        return new NoteException(404, "文档不存在：" + documentId);
    }

    /**
     * 操作失败异常
     * 
     * @param operation 操作类型
     * @return 异常实例
     */
    public static NoteException operationFailed(String operation) {
        return new NoteException(500, operation + "失败");
    }
}