package com.inyourcode.cluster;
/***
 *<pre>
 *     集群要解决的问题：
 *     1、集群关系
 *     2、节点管理：添加、移除、获取
 *     3、节点间的消息交互
 *
 *     集群关系的实现方式，下面列举两种：
 *     (1)、用配置文件的方式定义集群节点之间的关系
 *
 *      cluster1.conf :
 *      clusterId=1
 *      cluster[1]=127.0.0.1:8000//集群节点1的ip
 *      cluster[2]=127.0.0.1:8001//集群节点2的ip
 *      cluster[3]=127.0.0.1:8002//集群节点3的ip
 *      cluster[4]=127.0.0.1:803//集群节点4的ip
 *
 *      cluster2.conf :
 *      clusterId=2
 *      cluster[1]=127.0.0.1:8000
 *      cluster[2]=127.0.0.1:8001
 *      cluster[3]=127.0.0.1:8002
 *      cluster[4]=127.0.0.1:8003
 *
 *       .
 *       .
 *       .
 *
 *      cluster4.conf :
 *      clusterId=4
 *      cluster[1]=127.0.0.1:8000
 *      cluster[2]=127.0.0.1:8001
 *      cluster[3]=127.0.0.1:8002
 *      cluster[4]=127.0.0.1:8003
 *
 *      节点启动时，根据cluster[]配置，去连接对应的节点。
 *
 *      (2)、用一个中间件（如redis或自定义进程）来存储集群之间的关系
 *       cluster1.conf:
 *       clusterId=1
 *       clusterip=127.0.0.1:8000
 *
 *       .
 *       .
 *       .
 *
 *       cluster4.conf:
 *       clusterId=4
 *       clusterip=127.0.0.1:8003
 *
 *
 *     集群节点添加：
 *       节点启动时，会把自己节点信息注册到中间件。
 *
 *     集群节点的更新：
 *      节点注册后，每隔指定的时间，更新自己节点的信息，比如更新节点信息的时间
 *
 *     集群节点的获取：
 *       每隔指定的是时间从中间件中获取集群节点信息，从获取的节点信息中选择要连接的节点。
 *
 *     集群节点的删除
 *       每隔指定时间，会从中间件获取所有的节点，检查这些节点是否已经存活，没有存活的节点，则从中间件中删除
 *
 *
 *</pre>
 *
 ***/