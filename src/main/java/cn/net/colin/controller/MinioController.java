package cn.net.colin.controller;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.StreamProgress;
import cn.net.colin.entity.common.ResultCode;
import cn.net.colin.entity.common.ResultInfo;
import cn.net.colin.utils.MinioClientUtils;
import io.minio.Result;
import io.minio.messages.Item;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Package: cn.net.colin.controller
 * @Author: sxf
 * @Date: 2021-1-31
 * @Description:
 */
@Controller
@RequestMapping("/minio")
@ApiSort(value = 1)
@Api(tags = "Minio文件存储服务")
public class MinioController {
    Logger logger = LoggerFactory.getLogger(MinioController.class);
    @Resource
    private MinioClientUtils minioClientUtils;

    @GetMapping("/uploadObject")
    @ResponseBody
    @ApiOperationSupport(order = 1)
    @ApiOperation(value = "通过完整路径上传对象", notes = "上传文件到Minio",
            consumes = "application/x-www-form-urlencoded",produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(name="targetFileName",value="上传至minio服务器上的文件名",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="sourceFilePath",value="待上传文件源路径",required=true,paramType="query",dataType="String")
    })
    public ResultInfo uploadObject(String targetFileName,String sourceFilePath) throws IOException {
        Map<String,Object> resultMap = minioClientUtils.uploadObject(targetFileName,sourceFilePath);
        return ResultInfo.ofData(ResultCode.SUCCESS,resultMap);
    }

    @PostMapping("/putObject")
    @ResponseBody
    @ApiOperationSupport(order = 2)
    @ApiOperation(value = "通过InputStream上传对象")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "file",value = "文件上传",paramType = "formData",required = true,dataType = "file"),
            @ApiImplicitParam(name="targetFileName",value="上传至minio服务器上的文件名",required=false,paramType="query",dataType="String")
    })
    public ResultInfo putObject(@RequestParam("file") MultipartFile file,String targetFileName) throws IOException {
        if(targetFileName == null || (targetFileName != null && targetFileName.trim().equals(""))){
            targetFileName = file.getOriginalFilename();
        }
        Map<String,Object> resultMap = minioClientUtils.putObject(targetFileName,file.getInputStream(),file.getContentType());
        return ResultInfo.ofData(ResultCode.SUCCESS,resultMap);
    }

    @GetMapping("/downloadFile")
    @ApiOperationSupport(order = 3)
    @ApiOperation(value = "下载文件")
    @ApiImplicitParams({
            @ApiImplicitParam(name="targetFileName",value="文件名",required=true,paramType="query",dataType="String")
    })
    public void downloadFile(String targetFileName, HttpServletResponse response) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = minioClientUtils.getObject(targetFileName);
            //解决乱码
            targetFileName = URLEncoder.encode(targetFileName, "UTF-8");
            response.reset();
            // 设置文件名称
            response.setHeader("content-disposition", "attachment;fileName=\""+targetFileName+"\"");
            response.setContentType("application/octet-stream");
            response.setHeader("content-type", "application/octet-stream");
            out = response.getOutputStream();
            IoUtil.copyByNIO(in, out , 10240, new StreamProgress() {
                @Override
                public void start() {
                    logger.info("开始下载");
                }

                @Override
                public void progress(long l) {
                    logger.info("下载进度：{}", l);
                }

                @Override
                public void finish() {
                    logger.info("下载完成！");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(in != null){
                try {
                    in.close();
                    in = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(out != null){
                try {
                    out.close();
                    out = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @GetMapping("/objectExists")
    @ApiOperationSupport(order = 4)
    @ApiOperation(value = "文件是否存在")
    @ApiImplicitParams({
            @ApiImplicitParam(name="targetFileName",value="文件名",required=true,paramType="query",dataType="String")
    })
    @ResponseBody
    public ResultInfo objectExists(String targetFileName){
        boolean objectExists = false;
        Map<String,Object> resultMap = new HashMap<String,Object>();
        try {
            objectExists = minioClientUtils.objectExists(targetFileName);
            resultMap.put("exist",objectExists);
        }catch (Exception e){
            e.printStackTrace();
        }
        return ResultInfo.ofData(ResultCode.SUCCESS,resultMap);
    }

    @PostMapping("/removeObject")
    @ApiOperationSupport(order = 5)
    @ApiOperation(value = "文件删除")
    @ApiImplicitParams({
            @ApiImplicitParam(name="targetFileName",value="文件名",required=true,paramType="query",dataType="String")
    })
    @ResponseBody
    public ResultInfo removeObject(String targetFileName){
        boolean objectExists = false;
        Map<String,Object> resultMap = new HashMap<String,Object>();
        try {
            objectExists = minioClientUtils.removeObject(targetFileName);
            resultMap.put("exist",objectExists);
        }catch (Exception e){
            e.printStackTrace();
        }
        return ResultInfo.ofData(ResultCode.SUCCESS,resultMap);
    }

    @GetMapping("/listObjects")
    @ApiOperationSupport(order = 6)
    @ApiOperation(value = "获取存储桶中的对象集合")
    @ApiImplicitParams({
            @ApiImplicitParam(name="prefix",value="对象名称的前缀",required=false,paramType="query",dataType="String"),
            @ApiImplicitParam(name="startAfter",value="从某个对象开始检索",required=false,paramType="query",dataType="String")
    })
    @ResponseBody
    public ResultInfo listObjects(String prefix,String startAfter){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try {
            List<Map<String,Object>> dataList = new ArrayList<Map<String,Object>>();
            Iterable<Result<Item>> results = minioClientUtils.listObjects(prefix,startAfter);
            for (Result<Item> result : results) {
                Item item = result.get();
                if(!item.isDir()){
                    Map<String,Object> map = new HashMap<String,Object>();
                    map.put("lastModified",item.lastModified().withZoneSameInstant(ZoneId.systemDefault()).format(formatter));
                    map.put("size",item.size());
                    map.put("objectName",item.objectName());
                    dataList.add(map);
                }
            }
            return ResultInfo.ofDataAndTotal(ResultCode.SUCCESS,dataList,dataList.size());
        }catch (Exception e){
            e.printStackTrace();
            return ResultInfo.of(ResultCode.UNKNOWN_ERROR);
        }

    }

}
