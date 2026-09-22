package com.campus.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.dto.LoginFormDTO;
import com.campus.dto.Result;
import com.campus.dto.StudentDTO;
import com.campus.entity.Student;
import com.campus.entity.Post;
import com.campus.entity.Product;
import com.campus.entity.Favorite;
import com.campus.entity.Follow;
import com.campus.mapper.StudentMapper;
import com.campus.mapper.PostMapper;
import com.campus.mapper.ProductMapper;
import com.campus.mapper.FavoriteMapper;
import com.campus.mapper.FollowMapper;
import com.campus.service.IStudentService;
import com.campus.utils.RegexUtils;
import com.campus.utils.StudentHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.campus.utils.RedisConstants.*;

@Slf4j
@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student>
        implements IStudentService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${campus.dev-code:}")
    private String devCode;

    @Resource
    private PostMapper postMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private FavoriteMapper favoriteMapper;

    @Resource
    private FollowMapper followMapper;

    @Override
    public Result sendCode(String phone) {
        // 1. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }

        // 2. 生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 3. 保存到Redis
        stringRedisTemplate.opsForValue().set(
                LOGIN_CODE_KEY + phone,
                code,
                LOGIN_CODE_TTL,
                TimeUnit.MINUTES
        );

        // 4. 发送验证码（实际项目调用短信服务，这里打印日志）
        log.info("发送验证码：{} 到手机：{}", code, phone);

        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm) {
        String phone = loginForm.getPhone();
        String code = loginForm.getCode();

        // 1. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }

        // 2. 校验验证码（支持开发环境万能码）
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        boolean isDevCode = StrUtil.isNotBlank(devCode) && devCode.equals(code);
        if (!isDevCode && (StrUtil.isBlank(cacheCode) || !cacheCode.equals(code))) {
            return Result.fail("验证码错误");
        }

        // 3. 查询或创建用户
        Student student = lambdaQuery().eq(Student::getPhone, phone).one();
        if (student == null) {
            student = createUserWithPhone(phone);
        }

        // 4. 生成token
        String token = UUID.randomUUID().toString().replace("-", "");

        // 5. 转换为DTO并存入Redis
        StudentDTO studentDTO = BeanUtil.copyProperties(student, StudentDTO.class);
        Map<String, String> userMap = new HashMap<>();
        userMap.put("id", studentDTO.getId().toString());
        userMap.put("nickName", studentDTO.getNickName());
        userMap.put("avatar", studentDTO.getAvatar() != null ? studentDTO.getAvatar() : "");
        userMap.put("college", studentDTO.getCollege() != null ? studentDTO.getCollege() : "");
        userMap.put("credit", studentDTO.getCredit().toString());

        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 6. 删除验证码
        stringRedisTemplate.delete(LOGIN_CODE_KEY + phone);

        return Result.ok(token);
    }

    @Override
    public Result getInfo(Long id) {
        Student student = getById(id);
        if (student == null) {
            return Result.fail("用户不存在");
        }

        StudentDTO dto = BeanUtil.copyProperties(student, StudentDTO.class);
        return Result.ok(dto);
    }

    @Override
    @Transactional
    public Result updateInfo(StudentDTO studentDTO) {
        Long currentId = StudentHolder.getStudentId();
        if (currentId == null) {
            return Result.fail("未登录");
        }

        Student student = getById(currentId);
        if (student == null) {
            return Result.fail("用户不存在");
        }

        // 只允许更新部分字段
        student.setNickName(studentDTO.getNickName());
        student.setAvatar(studentDTO.getAvatar());
        student.setCollege(studentDTO.getCollege());
        student.setMajor(studentDTO.getMajor());

        boolean success = updateById(student);
        if (!success) {
            return Result.fail("更新失败");
        }

        return Result.ok(BeanUtil.copyProperties(student, StudentDTO.class));
    }

    @Override
    public Student createUserWithPhone(String phone) {
        Student student = new Student();
        student.setPhone(phone);
        student.setNickName("用户" + RandomUtil.randomString(6));
        student.setCredit(100);
        student.setStatus(1);
        save(student);
        return student;
    }

    @Override
    public Result getMyStats() {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("postCount", postMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Post>()
                        .eq(Post::getUserId, userId)));
        stats.put("productCount", productMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                        .eq(Product::getSellerId, userId)));
        stats.put("favoriteCount", favoriteMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)));
        stats.put("followerCount", followMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFolloweeId, userId)));
        stats.put("followeeCount", followMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Follow>()
                        .eq(Follow::getUserId, userId)));
        return Result.ok(stats);
    }
}