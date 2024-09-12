
package com.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.ShangbaoEntity;
import com.entity.YonghuEntity;
import com.entity.view.ShangbaoView;
import com.service.DictionaryService;
import com.service.ShangbaoService;
import com.service.TokenService;
import com.service.YonghuService;
import com.utils.PageUtils;
import com.utils.PoiUtil;
import com.utils.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * 上报
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/shangbao")
public class ShangbaoController {
    private static final Logger logger = LoggerFactory.getLogger(ShangbaoController.class);

    @Autowired
    private ShangbaoService shangbaoService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;

    //级联表service
    @Autowired
    private YonghuService yonghuService;



    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("用户".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        if(params.get("orderBy")==null || params.get("orderBy")==""){
            params.put("orderBy","id");
        }
        PageUtils page = shangbaoService.queryPage(params);

        //字典表数据转换
        List<ShangbaoView> list =(List<ShangbaoView>)page.getList();
        for(ShangbaoView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        ShangbaoEntity shangbao = shangbaoService.selectById(id);
        if(shangbao !=null){
            //entity转view
            ShangbaoView view = new ShangbaoView();
            BeanUtils.copyProperties( shangbao , view );//把实体数据重构到view中

                //级联表
                YonghuEntity yonghu = yonghuService.selectById(shangbao.getYonghuId());
                if(yonghu != null){
                    BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setYonghuId(yonghu.getId());
                }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody ShangbaoEntity shangbao, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,shangbao:{}",this.getClass().getName(),shangbao.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("用户".equals(role))
            shangbao.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

            shangbao.setInsertTime(new Date());
            shangbao.setCreateTime(new Date());
            shangbaoService.insert(shangbao);
            return R.ok();

    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody ShangbaoEntity shangbao, HttpServletRequest request){
        logger.debug("update方法:,,Controller:{},,shangbao:{}",this.getClass().getName(),shangbao.toString());

        shangbao.setUpdateTime(new Date());
            shangbaoService.updateById(shangbao);//根据id更新
            return R.ok();

    }

    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        shangbaoService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save(String fileName){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        try {
            List<ShangbaoEntity> shangbaoList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            ShangbaoEntity shangbaoEntity = new ShangbaoEntity();
//                            shangbaoEntity.setYonghuId(Integer.valueOf(data.get(0)));   //用户 要改的
//                            shangbaoEntity.setShangbaoName(data.get(0));                    //上报标题 要改的
//                            shangbaoEntity.setShangbaoText(data.get(0));                    //上报内容 要改的
//                            shangbaoEntity.setInsertTime(date);//时间
//                            shangbaoEntity.setReplyText(data.get(0));                    //回复内容 要改的
//                            shangbaoEntity.setUpdateTime(new Date(data.get(0)));          //回复时间 要改的
//                            shangbaoEntity.setCreateTime(date);//时间
                            shangbaoList.add(shangbaoEntity);


                            //把要查询是否重复的字段放入map中
                        }

                        //查询是否重复
                        shangbaoService.insertBatch(shangbaoList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }






}
