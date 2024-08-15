# coding: utf-8
# @File：Aida64SharedMem.py
# @Author：ly
# @Date ：2024/8/15
# @desc：基于aida64共享内存读取硬件信息

from xml.etree import ElementTree
import logging
import mmap
import sys
from xml.etree.ElementTree import ParseError


class Aida64(object):
    log = logging.getLogger(__name__)
    AIDA64_SHARED_MEMORY_NAME = "AIDA64_SensorValues"
    # 默认最大读取为16K
    bsz = 16 * 1024
    # 每次读取chunk块大小
    csz = 1024

    @classmethod
    def _parseBytes(cls, data):
        """解析数据"""
        for encoding in (sys.getdefaultencoding(), 'utf-8', 'gbk'):
            try:
                return data.decode(encoding=encoding)
            except UnicodeDecodeError:
                continue
        return data.decode()

    @classmethod
    def _readSharedMemData(cls) -> bytes:
        """从共享内存中读取self.bsz(默认16kb)长度内容"""
        with mmap.mmap(-1, cls.bsz,
                       tagname=cls.AIDA64_SHARED_MEMORY_NAME,
                       access=mmap.ACCESS_READ) as mm:
            content = bytearray()
            while True:
                chunk = mm.read(cls.csz)
                content.extend(chunk)
                if not chunk or chunk[-1] == 0:
                    break
            return bytes(content).rstrip(b'\x00')

    @classmethod
    def getData4Xml(cls) -> str:
        """获取aida64原生xml格式内容(最大16KB内容)"""
        content = cls._readSharedMemData()
        cls.log.info(f"aida64共享内存数据实际长度为{len(content)}")
        return (f"<root>"
                f"{cls._parseBytes(content)}"
                f"</root>")

    @classmethod
    def _xml2Dict(cls) -> dict:
        """将读取的xml转dict"""
        data = {}
        tree = ElementTree.fromstring(cls.getData4Xml())
        for item in tree:
            if item.tag not in data:
                data[item.tag] = []
            data[item.tag].append({
                key: item.find(key).text
                for key in ('id', 'label', 'value')
            })
        return data

    @classmethod
    def getData4Dict(cls) -> dict:
        """获取aida64字典格式内容"""
        try:
            data = cls._xml2Dict()
        except ParseError:
            cls.log.warning(f"预设长度 16KB 不够, 尝试使用 32KB 继续读取...")
            cls.bsz = cls.bsz * 2
            data = cls._xml2Dict()
        return data


if __name__ == '__main__':
    ad = Aida64
    # print(ad.getData4Xml())
    print(ad.getData4Xml())
    print(ad.getData4Dict())
