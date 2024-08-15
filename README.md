aida64共享内存监测数据读取python实现和java实现

+ python使用

  ```python
  from aida64.Aida64Util import Aida64 as ad
  
  print(ad.getData4Dict())#获取dict形式数据
  print(ad.getData4Xml())#获取xml形式数据
  ```

+ java使用

  ```java
  public static void main(String[] args) {
      System.out.println(Aida64Util.getSensorsInfoFromSharedMemory());//共享内存
      System.out.println(Aida64Util.getSensorsInfoFromRegistry());//注册表
  }
  ```

+ `sensors.xml`检测项数据部分说明

> 参考: 
>
> + [AIDA64 - External applications](https://aida64.co.uk/user-manual/file-menu/preferences/hardware-monitoring/external-applications)
> + [python_aida64](https://github.com/gwy15/python_aida64)