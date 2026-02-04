/**
 * 功能: 异常常量
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/18 下午7:08
 */
package com.telechat.constant;

public class ExceptionConstant {
    /**
     * 文件不存在异常状态码
     */
    public static final Integer FileNotFoundExceptionCode = 100404;
    public static final String FileNotFoundExceptionMsg = "文件不存在";

    /**
     * 文件上传异常状态码
     */
    public static final Integer FileUploadExceptionCode = 100500;
    public static final String FileUploadExceptionMsg = "文件上传失败";

    /**
     * 文件删除异常状态码
     */
    public static final Integer FileDeleteExceptionCode = 100501;
    public static final String FileDeleteExceptionMsg = "文件上传失败";

    /**
     * 文件超出最大限制异常状态码
     */
    public static final Integer FileExceededMaxSizeCode = 100502;
    public static final String FileExceededMaxSizeMsg = "文件超出最大限制";

    /**
     * 错误码
     */
    public static final Integer REGISTER_TYPE_ERROR_CODE = 500;
    public static final String REGISTER_TYPE_ERROR_MSG = "注册类型错误";

    public static final Integer VERIFY_CODE_ERROR_CODE = 500;
    public static final String VERIFY_CODE_ERROR_MSG = "验证码错误";

    public static final Integer LOGIN_ERROR_CODE = 500;
    public static final String LOGIN_ERROR_MSG = "登录失败";

    public static final Integer ResetPassword_TYPE_ERROR_CODE = 500;
    public static final String ResetPassword_TYPE_ERROR_MSG = "重置密码类型错误";

    public static final Integer SERVER_ERROR_CODE = 500;
    public static final String SERVER_ERROR_MSG = "服务器错误";

    public static Integer Judge_Query_Exception_Code = 130;
    public static String Judge_Query_Exception_MSG = "参数校验错误";

    public static String CONTACT_ALREADY_HANDLE_EXCEPTION_MSG = "该联系申请已处理";

    /**
     * 已重复存在类型错误码
     */
    public static final Integer ALREADY_EXIST_CODE = 400;
    public static final Integer VERIFY_CODE_EXIST_CODE = 400;
    public static final Integer EMAIL_EXIST_CODE = 400;

    public static final String CONTACT_ALREADY_EXIST_MSG = "该用户已是你的联系人";



    /**
     * 未允许类型错误码
     */
    public static final Integer NOT_ALLOWED_CODE = 403;
    public static final String NOT_ALLOWED_MSG = "未允许";
    public static final String CONTACT_NOT_ALLOWED_MSG = "不允许操作该联系申请";
    public static final String NOT_ALLOWED_SEND_APPLY_MYSELF = "不允许添加自己为好友";


    /**
     * 未找到类型错误码
     */
    public static final int NOT_EXIST_CODE = 404;
    public static final String NOT_EXIST_MSG = "不存在";
    public static final String CONTACT_NOT_EXIST_MSG = "联系不存在";
    public static final String USER_NOT_EXIST_MSG = "该联系人不存在";
    public static final String CONVERSATION_NOT_EXIST_MSG = "联系人对应的会话不存在";


    /**
     * 用户未登录
     * 需要清除token并重新登录
     *
     */
    public static final Integer USER_NOT_LOGIN_ERROR_CODE = 101;
}
